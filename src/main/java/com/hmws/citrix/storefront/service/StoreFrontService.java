package com.hmws.citrix.storefront.service;

import com.hmws.citrix.storefront.dto.LoginFormDto;
import com.hmws.citrix.storefront.dto.RequirementDto;
import com.hmws.citrix.storefront.dto.StoreFrontAuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreFrontService {

    private static final String BASE_URL = "http://172.24.247.151/Citrix/hmstoreWeb";
    private final RestTemplate restTemplate;
    private String csrfToken;
    private String sessionId;

    @Transactional
    public Map<String, Object> getStoreFrontLoginForm() {

        log.info("fetchSiteConfiguration service entrance");

        HttpHeaders headers = new HttpHeaders();

        headers.set(HttpHeaders.USER_AGENT, "My Application");
        headers.set("X-Citrix-IsUsingHTTPS", "No");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/Home/Configuration",
                HttpMethod.POST,
                entity,
                String.class
        );

        try {
            extractCookiesFromHeaders(response.getHeaders());

        } catch (Exception e) {
            log.error("Failed to extract cookies from headers", e);
        }

        Set<String> authMethods = getAuthMethods(this.csrfToken, this.sessionId);

        String explicitFormMethod = authMethods.stream()
                .filter(method -> method.equals("ExplicitForms"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ExplicitForms authentication method not found"));

        getExplicitLoginForm(explicitFormMethod, this.csrfToken, this.sessionId);

        LoginFormDto loginForm = getExplicitLoginForm(explicitFormMethod, this.csrfToken, this.sessionId);

        if (this.csrfToken != null && this.sessionId != null && loginForm != null) {
            return Map.of(
                    "csrfToken", this.csrfToken,
                    "sessionId", this.sessionId,
                    "authMethods", authMethods,
                    "LoginForm", loginForm
            );

        } else {
            throw new RuntimeException("Required data is missing");
        }
    }

    private Set<String> getAuthMethods(String csrfToken, String sessionId) {

        HttpHeaders headers = new HttpHeaders();

        headers.set("Cookie", "CsrfToken=" + csrfToken + "; ASP.NET_SessionId=" + sessionId);
        headers.set("Csrf-Token", csrfToken);
        headers.set(HttpHeaders.USER_AGENT, "My Application");
        headers.set("X-Citrix-IsUsingHTTPS", "No");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        Set<String> methodNames = new HashSet<>();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/Authentication/GetAuthMethods",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(response.getBody())));

            NodeList methodNodes = document.getElementsByTagName("method");
            for (int i = 0; i < methodNodes.getLength(); i++) {
                Element methodElement = (Element) methodNodes.item(i);
                String methodName = methodElement.getAttribute("name");
                String methodUrl = methodElement.getAttribute("url");
                methodNames.add(methodName);
                log.info("Found auth method: {} with URL: {}", methodName, methodUrl);
            }
        } catch (Exception e) {
            log.error("Failed to parse auth methods XML", e);
        }
        return methodNames;
    }

    private LoginFormDto getExplicitLoginForm(String method, String csrfToken, String sessionId) {

        HttpHeaders headers = new HttpHeaders();

        // 필수 헤더들 설정
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
        headers.set(HttpHeaders.ACCEPT, "application/xml, text/xml, */*; q=0.01");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-us,en;q=0.7,fr;q=0.3");
        headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
        headers.set("Csrf-Token", csrfToken);
        headers.set("X-Citrix-IsUsingHTTPS", "No");
        headers.set("X-Requested-With", "XMLHttpRequest");
        headers.set(HttpHeaders.REFERER, BASE_URL);
        headers.set(HttpHeaders.CONNECTION, "keep-alive");
        headers.set(HttpHeaders.PRAGMA, "no-cache");
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");

        // 쿠키 설정
        headers.set(HttpHeaders.COOKIE,
                String.format("CtxsAuthMethod=%s; CsrfToken=%s; ASP.NET_SessionId=%s",
                        method,
                        csrfToken,
                        sessionId)
        );

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    BASE_URL + "/ExplicitAuth/Login",
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );

            log.info("Response body: {}", response.getBody());

            byte[] responseBody = response.getBody();
            responseBody = decompressGzip(responseBody);

            log.info("Response body: {}", byteArrayToHexString(responseBody));

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {

                log.info("getExplicitLoginForm if entrance");
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                log.info("DocBuilder success check");

                InputStream inputStream = new ByteArrayInputStream(responseBody);
                log.info("InputStream success check");
                log.info("inputStream: {}", inputStream);

                Document document = builder.parse(inputStream);
                log.info("Document success check");

                LoginFormDto loginForm = parseLoginForm(document);

                return loginForm;
            }
            log.error("Response body is null");
            return null;

        } catch (Exception e) {
            log.error("Failed to parse login form XML", e);
            return null;
        }

    }

    public StoreFrontAuthResponse authenticateUser(String username, String password, boolean saveCredentials) {

        String requestXml = String.format("""
        <?xml version="1.0" encoding="UTF-8"?>
        <AuthenticationData xmlns="http://citrix.com/authentication/response/1">
            <Credentials>
                <Credential><ID>username</ID><Value>%s</Value></Credential>
                <Credential><ID>password</ID><Value>%s</Value></Credential>
                <Credential><ID>saveCredentials</ID><Value>%s</Value></Credential>
            </Credentials>
        </AuthenticationData>
        """, username, password, saveCredentials);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.set("CsrfToken", this.csrfToken);
        headers.set("X-Citrix-IsUsingHTTPS", "No");

        HttpEntity<String> request = new HttpEntity<>(requestXml, headers);

        return restTemplate.postForObject(
                BASE_URL + "/ExplicitAuth/LoginAttempt",
                request,
                StoreFrontAuthResponse.class
        );
    }

    private void extractCookiesFromHeaders(HttpHeaders headers) throws Exception {
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.contains("CsrfToken=")) {
                    this.csrfToken = cookie.split("CsrfToken=")[1].split(";")[0];
                    log.info("CSRF TOKEN: {}", this.csrfToken);
                }
                if (cookie.contains("ASP.NET_SessionId=")) {
                    this.sessionId = cookie.split("ASP.NET_SessionId=")[1].split(";")[0];
                    log.info("SESSION ID: " + this.sessionId);
                }
            }
        }
    }

    private byte[] decompressGzip(byte[] compressedData) throws IOException {
        ByteArrayInputStream byteInput = new ByteArrayInputStream(compressedData);
        GZIPInputStream gzipInput = new GZIPInputStream(byteInput);
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = gzipInput.read(buffer)) > 0) {
            byteOutput.write(buffer, 0, length);
        }
        byteOutput.close();
        gzipInput.close();
        return byteOutput.toByteArray();
    }

    private String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    private LoginFormDto parseLoginForm(Document document) {

        Element root = document.getDocumentElement();
        Element authReq = (Element) root.getElementsByTagName("AuthenticationRequirements").item(0);

        String postBackUrl = authReq.getElementsByTagName("PostBack").item(0).getTextContent();
        String cancelUrl = authReq.getElementsByTagName("CancelPostBack").item(0).getTextContent();

        NodeList requirements = document.getElementsByTagName("Requirement");
        List<RequirementDto> reqDtos = new ArrayList<>();

        for (int i = 0; i < requirements.getLength(); i++) {
            Element req = (Element) requirements.item(i);
            Element credential = (Element) req.getElementsByTagName("Credential").item(0);

            reqDtos.add(RequirementDto.builder()
                    .id(getElementContent(credential, "ID"))
                    .type(getElementContent(credential, "Type"))
                    .label(getElementContent(req, "Text"))
                    .initialValue(getElementContent(req, "InitialValue"))
                    .build());
        }

        return LoginFormDto.builder()
                .postBackUrl(postBackUrl)
                .cancelUrl(cancelUrl)
                .requirements(reqDtos)
                .build();

    }

    private String getElementContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null) {
                return node.getTextContent();
            }
        }
        return "";
    }
}
