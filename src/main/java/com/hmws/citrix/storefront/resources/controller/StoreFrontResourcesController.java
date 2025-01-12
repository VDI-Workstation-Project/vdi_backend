package com.hmws.citrix.storefront.resources.controller;

import com.hmws.citrix.storefront.resources.dto.StoreFrontResourcesDto;
import com.hmws.citrix.storefront.resources.service.StoreFrontResourcesService;
import com.hmws.global.authentication.TokenProvider;
import com.hmws.global.exception.ErrorResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/storefront/resources")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class StoreFrontResourcesController {

    private final StoreFrontResourcesService storeFrontResourcesService;
    private final TokenProvider tokenProvider;

    @PostMapping("/list")
    public ResponseEntity<?> getResources(HttpServletRequest request) {

        try {
            String token = tokenProvider.resolveToken(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(401, "토큰이 없습니다."));
            }

            Claims claims = tokenProvider.getClaims(token);
            String csrfToken = (String) claims.get("citrixCsrfToken");
            String sessionId = (String) claims.get("citrixSessionId");
            String authId = (String) claims.get("citrixAuthId");

            List<StoreFrontResourcesDto> resources = storeFrontResourcesService.getResources(
                    csrfToken,
                    sessionId,
                    authId
            );

            return ResponseEntity.ok(resources);

        } catch (Exception e) {
            log.error("Failed to get resources", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "리소스 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
