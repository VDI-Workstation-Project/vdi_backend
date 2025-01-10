package com.hmws.citrix.storefront.session;

import com.hmws.citrix.storefront.service.StoreFrontService;
import com.hmws.global.authentication.TokenProvider;
import com.hmws.global.authentication.dto.AuthUserDto;
import com.hmws.global.authentication.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CitrixSessionManager {

    private final StoreFrontService storeFrontService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(fixedRate = 45 * 60 * 1000) // 45분 = 2,700,000 밀리초
    public void keepAliveSessions() {
        refreshTokenRepository.findAll().forEach(refreshToken -> {
            try {
                Claims claims = tokenProvider.getClaims(refreshToken.getToken());

                boolean sessionValid = storeFrontService.keepAliveSession(
                        (String) claims.get("citrixSessionId"),
                        (String) claims.get("citrixCsrfToken")
                );

                if (sessionValid) {
                    // 새로운 액세스 토큰 생성
                    AuthUserDto authUser = AuthUserDto.builder()
                            .username(refreshToken.getUsername())
                            .citrixCsrfToken((String) claims.get("citrixCsrfToken"))
                            .citrixSessionId((String) claims.get("citrixSessionId"))
                            .citrixAuthId((String) claims.get("citrixAuthId"))
                            .build();

                    String newAccessToken = tokenProvider.generateToken(authUser);
                    refreshToken.updateAccessToken(newAccessToken);
                    refreshTokenRepository.save(refreshToken);

                } else {
                    refreshTokenRepository.delete(refreshToken);
                    log.warn("Session expired for user: {}", refreshToken.getUsername());
                }

            } catch (Exception e) {
                log.error("Failed to refresh session for user: " + refreshToken.getUsername(), e);
            }
        });
        log.info("Scheduled session refresh completed");
    }
}
