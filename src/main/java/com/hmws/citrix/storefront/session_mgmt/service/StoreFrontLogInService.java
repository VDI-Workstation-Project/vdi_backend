package com.hmws.citrix.storefront.session_mgmt.service;

import com.hmws.citrix.storefront.session_mgmt.dto.PasswordChangeRequest;
import com.hmws.citrix.storefront.session_mgmt.dto.StoreFrontAuthResponse;
import com.hmws.citrix.storefront.session_mgmt.session.StoreFrontSession;
import com.hmws.citrix.storefront.session_mgmt.session.StoreFrontSessionService;
import com.hmws.global.authentication.dto.AuthUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreFrontLogInService {

    @Value("${citrix.storefront.server.base-url}")
    private String storeFrontBaseUrl;

    private final RestTemplate restTemplate;
    private final StoreFrontSessionService sessionService;

    private static final ThreadLocal<StoreFrontSession> citrixSession = new ThreadLocal<>();

    @Transactional
    public StoreFrontAuthResponse storeFrontLogin(String username, String password, boolean saveCredentials) {

        try {

            HttpHeaders headers = new HttpHeaders();

            headers.set(HttpHeaders.USER_AGENT, "My Application");
            headers.set("X-Citrix-IsUsingHTTPS", "No");

            ResponseEntity<String> response = restTemplate.exchange(
                    storeFrontBaseUrl + "/Home/Configuration",
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    String.class
            );

            // 세션 정보 추출 및 Redis에 저장
            StoreFrontSession session = extractCookiesFromHeaders(response.getHeaders());

            proceedAuthMethods(session.getCsrfToken(), session.getSessionId());

            proceedExplicitLoginForm(session.getCsrfToken(), session.getSessionId());

            StoreFrontAuthResponse authResponse =  performLoginAttempt(username, password, saveCredentials, session);

            // 비밀번호 변경이 필요하지 않은 경우 Redis에서 세션 제거
            if (!"update-credentials".equals(authResponse.getResult())) {
                sessionService.removeSession(session.getSessionId());
            }

            return authResponse;

        } catch (Exception e) {
            log.error("Login failed", e);
            StoreFrontAuthResponse errorResponse = new StoreFrontAuthResponse();
            errorResponse.setMessage("Login failed: " + e.getMessage());
            return errorResponse;

        }

    }

    public StoreFrontAuthResponse performLoginAttempt(String username, String password, boolean saveCredentials, StoreFrontSession session) {

        String requestBody = String.format("username=%s&password=%s&saveCredentials=%s&loginBtn=Log+On&StateContext=",
                URLEncoder.encode(username, StandardCharsets.UTF_8),
                URLEncoder.encode(password, StandardCharsets.UTF_8),
                saveCredentials);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", String.format("CtxsAuthMethod=ExplicitForms; CsrfToken=%s; ASP.NET_SessionId=%s",
                session.getCsrfToken(), session.getSessionId()));
        headers.set("Csrf-Token", session.getCsrfToken());
        headers.set("X-Citrix-IsUsingHTTPS", "No");
        headers.set("X-Requested-With", "XMLHttpRequest");
        headers.set("Accept", "application/xml, text/xml, */*; q=0.01");

        log.info("authenticateUser service request body: {}", requestBody);
        log.info("authenticateUser service headers: {}", headers);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    storeFrontBaseUrl + "/ExplicitAuth/LoginAttempt",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Response body: {}", response.getBody());
            log.info("Response Headers: {}", response.getHeaders());

            // CtxsAuthId 추출 및 설정
            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.contains("CtxsAuthId=")) {
                        String ctxsAuthId = cookie.split("CtxsAuthId=")[1].split(";")[0];
                        session.setCtxsAuthId(ctxsAuthId);
                        sessionService.saveSession(session);
                    break;
                    }
                }
            }

            StoreFrontAuthResponse authResponse = new StoreFrontAuthResponse();
            authResponse.setHttpStatus(HttpStatus.valueOf(response.getStatusCode().value()));

            authResponse.setSessionId(session.getSessionId());
            authResponse.setCsrfToken(session.getCsrfToken());
            authResponse.setCtxsAuthId(session.getCtxsAuthId());

            // XML 응답을 파싱하여 StoreFrontAuthResponse 객체로 변환
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(response.getBody())));

            log.info("document: {}", document);

            // Result 값 추출
            NodeList resultNodes = document.getElementsByTagName("Result");
            if (resultNodes.getLength() > 0) {
                authResponse.setResult(resultNodes.item(0).getTextContent());
            }

            // AuthType 값 추출
            NodeList authTypeNodes = document.getElementsByTagName("AuthType");
            if (authTypeNodes.getLength() > 0) {
                authResponse.setAuthType(authTypeNodes.item(0).getTextContent());
            }

            // 에러 메시지 추출 (있는 경우)
            NodeList errorNodes = document.getElementsByTagName("LogMessage");
            if (errorNodes.getLength() > 0) {
                authResponse.setMessage(errorNodes.item(0).getTextContent());
            }

            NodeList errorLabels = document.getElementsByTagName("Label");
            for (int i = 0; i < errorLabels.getLength(); i++) {
                Element label = (Element) errorLabels.item(i);
                NodeList typeNodes = label.getElementsByTagName("Type");
                NodeList textNodes = label.getElementsByTagName("Text");

                if (typeNodes.getLength() > 0 && "error".equals(typeNodes.item(0).getTextContent()) &&
                textNodes.getLength() > 0) {
                    String errorMessage = textNodes.item(0).getTextContent();
                    authResponse.setMessage(errorMessage);
                    authResponse.setResult("error");
                    break;
                }
            }

            return authResponse;

        } catch (Exception e) {
            log.error("Authentication failed", e);
            StoreFrontAuthResponse errorResponse = new StoreFrontAuthResponse();
            errorResponse.setMessage("Authentication failed: " + e.getMessage());
            return errorResponse;
        }
    }

    public boolean keepAliveSession(String sessionId, String csrfToken) {

        log.info("keepAliveSession entrance");

        HttpHeaders headers = new HttpHeaders();

        headers.set("Cookie", String.format("CsrfToken=%s; ASP.NET_SessionId=%s",
                csrfToken, sessionId));
        headers.set("Csrf-Token", csrfToken);
        headers.set("X-Citrix-IsUsingHTTPS", "No");
        headers.set("X-Requested-With", "XMLHttpRequest");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    storeFrontBaseUrl + "/Home/KeepAlive",
                    HttpMethod.HEAD,
                    new HttpEntity<>(headers),
                    String.class
            );

            log.info("keepAliveSession response: {}", response);

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("KeepAlive request failed", e);
            return false;
        }
    }

    public boolean logout(AuthUserDto authUser) {

        HttpHeaders headers = new HttpHeaders();

        headers.set("Csrf-Token", authUser.getCitrixCsrfToken());
        headers.set("X-Citrix-IsUsingHTTPS", "No");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    storeFrontBaseUrl + "/Authentication/Logoff",
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Logout request failed", e);
            return false;
        }

    }

    @Transactional
    public StoreFrontAuthResponse changePassword(PasswordChangeRequest request) {

        // Redis에서 세션 조회
        StoreFrontSession session = sessionService.getSession(request.getSessionId());
        if (session == null) {
            throw new RuntimeException("No active session found");
        }

        String requestBody = String.format("oldPassword=%s&newPassword=%s&confirmPassword=%s&changePasswordBtn=OK&StateContext=",
                URLEncoder.encode(request.getOldPassword(), StandardCharsets.UTF_8),
                URLEncoder.encode(request.getNewPassword(), StandardCharsets.UTF_8),
                URLEncoder.encode(request.getConfirmPassword(), StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", String.format("CtxsAuthMethod=ExplicitForms; CsrfToken=%s; ASP.NET_SessionId=%s",
                session.getCsrfToken(), session.getSessionId()));
        headers.set("Csrf-Token", session.getCsrfToken());
        headers.set("X-Citrix-IsUsingHTTPS", "No");
        headers.set("X-Requested-With", "XMLHttpRequest");
        headers.set("Accept", "application/xml, text/xml, */*; q=0.01");

        log.info("requestBody: {}", requestBody);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    storeFrontBaseUrl + "/ExplicitAuth/SendForm",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("changePassword response: {}", response);

            HttpStatus httpStatus = HttpStatus.valueOf(response.getStatusCode().value());

            StoreFrontAuthResponse authResponse =  parseAuthResponse(httpStatus, response.getBody());

            // 비밀번호 변경이 성공한 경우에만 세션 제거
            if ("success".equals(authResponse.getResult())) {
                log.info("비밀번호 변경 성공. 세션 제거: {}", session.getSessionId());
                sessionService.removeSession(session.getSessionId());
            } else {
                log.warn("비밀번호 변경 실패. 세션 유지: {}", session.getSessionId());
            }

            return authResponse;

        } catch (Exception e) {
            log.error("Password change failed", e);
            StoreFrontAuthResponse errorResponse = new StoreFrontAuthResponse();
            errorResponse.setMessage("Password change failed: " + e.getMessage());
            return errorResponse;
        }
    }

    private StoreFrontSession extractCookiesFromHeaders(HttpHeaders headers) throws Exception {
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        String csrfToken = null;
        String sessionId = null;
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.contains("CsrfToken=")) {
                    csrfToken = cookie.split("CsrfToken=")[1].split(";")[0];
                    log.info("CSRF TOKEN: {}", csrfToken);
                }
                if (cookie.contains("ASP.NET_SessionId=")) {
                    sessionId = cookie.split("ASP.NET_SessionId=")[1].split(";")[0];
                    log.info("SESSION ID: " + sessionId);
                }
            }
        }
        StoreFrontSession session = new StoreFrontSession(csrfToken, sessionId);
        sessionService.saveSession(session);

        return session;
    }

    private void proceedAuthMethods(String csrfToken, String sessionId) {

        HttpHeaders headers = new HttpHeaders();

        headers.set("Cookie", "CsrfToken=" + csrfToken + "; ASP.NET_SessionId=" + sessionId);
        headers.set("Csrf-Token", csrfToken);
        headers.set(HttpHeaders.USER_AGENT, "My Application");
        headers.set("X-Citrix-IsUsingHTTPS", "No");

        try {
            restTemplate.execute(
                    storeFrontBaseUrl + "/Authentication/GetAuthMethods",
                    HttpMethod.POST,
                    request -> {
                        request.getHeaders().putAll(headers);
                    },
                    null
            );

        } catch (Exception e) {
            log.error("Failed to execute auth methods request", e);
        }
    }

    private void proceedExplicitLoginForm(String csrfToken, String sessionId) {
        HttpHeaders headers = new HttpHeaders();

        // 필수 헤더들 설정
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
        headers.set(HttpHeaders.ACCEPT, "application/xml, text/xml, */*; q=0.01");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-us,en;q=0.7,fr;q=0.3");
        headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
        headers.set("Csrf-Token", csrfToken);
        headers.set("X-Citrix-IsUsingHTTPS", "No");
        headers.set("X-Requested-With", "XMLHttpRequest");
        headers.set(HttpHeaders.REFERER, storeFrontBaseUrl);
        headers.set(HttpHeaders.CONNECTION, "keep-alive");
        headers.set(HttpHeaders.PRAGMA, "no-cache");
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");

        // 쿠키 설정
        headers.set(HttpHeaders.COOKIE,
                String.format("CtxsAuthMethod=ExplicitForms; CsrfToken=%s; ASP.NET_SessionId=%s",
                        csrfToken,
                        sessionId)
        );

        try {
            restTemplate.execute(
                    storeFrontBaseUrl + "/ExplicitAuth/Login",
                    HttpMethod.POST,
                    request -> {
                        request.getHeaders().putAll(headers);
                    },
                    null
            );

        } catch (Exception e) {
            log.error("Failed to execute login form request", e);
        }
    }

    private StoreFrontAuthResponse parseAuthResponse(HttpStatus httpStatus, String xmlResponse) {

        log.info("parseAuthResponse entrance");

        StoreFrontAuthResponse authResponse = new StoreFrontAuthResponse();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // XXE 공격 방지
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlResponse)));

            // Http Response 저장
            authResponse.setHttpStatus(httpStatus);

            // Status 파싱
            NodeList statusNodes = document.getElementsByTagName("Status");
            if (statusNodes.getLength() > 0) {
                String status = statusNodes.item(0).getTextContent();
                authResponse.setResponseStatus(status.toLowerCase());
            }

            // Result 파싱
            NodeList resultNodes = document.getElementsByTagName("Result");
            if (resultNodes.getLength() > 0) {
                String result = resultNodes.item(0).getTextContent();
                authResponse.setResult(result.toLowerCase());

                // more-info인 경우 성공 메시지 확인
                if ("more-info".equals(result)) {
                    NodeList labelNodes = document.getElementsByTagName("Text");
                    for (int i = 0; i < labelNodes.getLength(); i++) {
                        String text = labelNodes.item(i).getTextContent();
                        if (text.contains("successfully")) {
                            authResponse.setResult("success");
                            break;
                        }
                    }
                }
            }

            // 에러 메시지나 확인 메시지 파싱
            NodeList textNodes = document.getElementsByTagName("Text");
            if (textNodes.getLength() > 0) {
                authResponse.setMessage(textNodes.item(0).getTextContent());
            }

            log.info("authResponse httpStatus: {}", authResponse.getHttpStatus());
            log.info("authResponse result: {}", authResponse.getResult());
            log.info("authResponse errorMessage: {}", authResponse.getMessage());

            return authResponse;

        } catch (Exception e) {
            log.error("Failed to parse authentication response", e);
            authResponse.setResult("error");
            authResponse.setMessage("Failed to parse response: " + e.getMessage());
            return authResponse;
        }

    }

}


