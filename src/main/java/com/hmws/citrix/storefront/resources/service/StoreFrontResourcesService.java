package com.hmws.citrix.storefront.resources.service;

import com.hmws.citrix.storefront.resources.dto.StoreFrontResourcesDto;
import com.hmws.citrix.storefront.resources.dto.StoreFrontResourcesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreFrontResourcesService {

    private static final String BASE_URL = "http://172.24.247.151/Citrix/hmstoreWeb";
    private final RestTemplate restTemplate;

    public List<StoreFrontResourcesDto> getResources(String csrfToken, String sessionId, String authId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", String.format("CsrfToken=%s; ASP.NET_SessionId=%s; CtxsAuthId=%s",
                csrfToken, sessionId, authId));
        headers.set("Csrf-Token", csrfToken);
        headers.set("X-Citrix-IsUsingHTTPS", "No");
        headers.set("User-Agent", "My Application");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<StoreFrontResourcesResponse> response = restTemplate.exchange(
                    BASE_URL + "/Resources/List",
                    HttpMethod.POST,
                    entity,
                    StoreFrontResourcesResponse.class
            );

            // CtxsDeviceId 추출
            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.contains("CtxsDeviceId=")) {
                        String deviceId = cookie.split("CtxsDeviceId=")[1].split(";")[0];
                        log.info("Retrieved CtxsDeviceId: {}", deviceId);
                        break;
                    }
                }
            }

            return response.getBody() != null ? response.getBody().getResources() : new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to fetch resources", e);
            throw new RuntimeException("Failed to fetch resources: " + e.getMessage());
        }
    }
}
