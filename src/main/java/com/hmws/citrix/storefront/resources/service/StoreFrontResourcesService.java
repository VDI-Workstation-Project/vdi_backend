package com.hmws.citrix.storefront.resources.service;

import com.hmws.citrix.storefront.resources.dto.StoreFrontLaunchInfo;
import com.hmws.citrix.storefront.resources.dto.StoreFrontResourcesDto;
import com.hmws.citrix.storefront.resources.dto.StoreFrontResourcesResponse;
import com.hmws.citrix.storefront.resources.repository.StoreFrontLaunchInfoRepository;
import com.hmws.global.authentication.dto.AuthUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreFrontResourcesService {

    @Value("${citrix.storefront.server.base-url}")
    private String storeFrontBaseUrl;

    private final RestTemplate restTemplate;
    private final StoreFrontLaunchInfoRepository launchInfoRepository;

    public List<StoreFrontResourcesDto> getResources(AuthUserDto authUser) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", String.format("CsrfToken=%s; ASP.NET_SessionId=%s; CtxsAuthId=%s",
                authUser.getCitrixCsrfToken(),
                authUser.getCitrixSessionId(),
                authUser.getCitrixAuthId()));
        headers.set("Csrf-Token", authUser.getCitrixCsrfToken());
        headers.set("X-Citrix-IsUsingHTTPS", "No");
        headers.set("User-Agent", "My Application");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<StoreFrontResourcesResponse> response = restTemplate.exchange(
                    storeFrontBaseUrl + "/Resources/List",
                    HttpMethod.POST,
                    entity,
                    StoreFrontResourcesResponse.class);

            String ctxsDeviceId = extractDeviceId(response.getHeaders());

            if (response.getBody() != null && !response.getBody().getResources().isEmpty()) {
                StoreFrontResourcesDto resource = response.getBody().getResources().get(0);

                StoreFrontLaunchInfo launchInfo = StoreFrontLaunchInfo.builder()
                        .ctxsDeviceId(ctxsDeviceId)
                        .launchStatusUrl(resource.getLaunchstatusurl())
                        .launchUrl(resource.getLaunchurl())
                        .desktopHostname(resource.getDesktophostname())
                        .resourceId(resource.getId())
                        .createdAt(LocalDateTime.now())
                        .build();

                // 현재 사용자의 username으로 저장
                String username = authUser.getUsername();
                launchInfoRepository.save(username, launchInfo);
            }

            return response.getBody() != null ? response.getBody().getResources() : new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to fetch resources", e);
            throw new RuntimeException("Failed to fetch resources: " + e.getMessage());
        }
    }

    public Map<String, String> checkLaunchStatus(AuthUserDto authUser) {

        log.info("Check LaunchStatus started...........");

        StoreFrontLaunchInfo launchInfo = launchInfoRepository.findByUsername(authUser.getUsername());
        if (launchInfo == null) {
            throw new RuntimeException("Launch information not found");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", String.format("CtxsDeviceId=%s; CsrfToken=%s; ASP.NET_SessionId=%s; CtxsAuthId=%s",
                launchInfo.getCtxsDeviceId(),
                authUser.getCitrixCsrfToken(),
                authUser.getCitrixSessionId(),
                authUser.getCitrixAuthId()));
        headers.set("Csrf-Token", authUser.getCitrixCsrfToken());
        headers.set("X-Citrix-IsUsingHTTPS", "No");
        headers.set("User-Agent", "My Application");

        // Request body 추가
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("displayNameDesktopTitle", launchInfo.getDesktopHostname());

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        log.info("Launch Status Request Headers {}", requestEntity.getHeaders());
        log.info("Launch Status Request Entity - Body {}", requestEntity.getBody());

        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                storeFrontBaseUrl + "/" + launchInfo.getLaunchStatusUrl(),
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Map<String, String>>() {
                });

        log.info("Check LaunchStatus ended........... {}", response.getBody());

        return response.getBody();
    }

    public byte[] getLaunchIca(AuthUserDto authUser) {

        log.info("Get LaunchIca started...........");

        StoreFrontLaunchInfo launchInfo = launchInfoRepository.findByUsername(authUser.getUsername());
        if (launchInfo == null) {
            throw new RuntimeException("Launch information not found");
        }

        String launchId = UUID.randomUUID().toString();
        String queryParams = String.format("?displayNameDesktopTitle=%s&launchId=%s&CsrfToken=%s&IsUsingHttps=No",
                URLEncoder.encode(launchInfo.getDesktopHostname(), StandardCharsets.UTF_8),
                launchId,
                authUser.getCitrixCsrfToken());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", String.format("CtxsDeviceId=%s; CsrfToken=%s; ASP.NET_SessionId=%s; CtxsAuthId=%s",
                launchInfo.getCtxsDeviceId(),
                authUser.getCitrixCsrfToken(),
                authUser.getCitrixSessionId(),
                authUser.getCitrixAuthId()));
        headers.set("User-Agent", "My Application");
        headers.set("X-Citrix-IsUsingHTTPS", "No");

        ResponseEntity<byte[]> response = restTemplate.exchange(
                storeFrontBaseUrl + "/" + launchInfo.getLaunchUrl() + queryParams,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class);

        log.info("Get LaunchIca ended........... {}", response.getBody());

        return response.getBody();
    }

    // CtxsDeviceId 추출
    private String extractDeviceId(HttpHeaders headers) {

        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.contains("CtxsDeviceId=")) {
                    return cookie.split("CtxsDeviceId=")[1].split(";")[0];
                }
            }
        }
        return null;
    }
}
