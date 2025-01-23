package com.hmws.citrix.storefront.session_mgmt.session;

import com.hmws.citrix.storefront.session_mgmt.service.StoreFrontLogInService;
import com.hmws.global.authentication.TokenProvider;
import com.hmws.global.authentication.dto.AuthUserDto;
import com.hmws.global.authentication.repository.RefreshTokenRepository;
import com.hmws.usermgmt.domain.UserData;
import com.hmws.usermgmt.repository.UserDataRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreFrontSessionManager {

    private final StoreFrontLogInService storeFrontLogInService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDataRepository userDataRepository;

    @Scheduled(fixedRate = 3300000) // 55분
    public void keepAliveSessions() {

        log.info("keepAliveSessions auto started");

        refreshTokenRepository.findAll().forEach(refreshToken -> {
            try {
                Claims claims = tokenProvider.getClaims(refreshToken.getToken());
                String username = refreshToken.getUsername();

                boolean sessionValid = storeFrontLogInService.keepAliveSession(
                        (String) claims.get("citrixSessionId"),
                        (String) claims.get("citrixCsrfToken")
                );

                if (sessionValid) {
                    // UserData 조회하여 userType과 userRole 가져오기
                    UserData userData = userDataRepository.findByEmail(username)
                            .orElseThrow(() -> new RuntimeException("User not found: " + username));

                    // 새로운 액세스 토큰 생성
                    AuthUserDto authUser = AuthUserDto.builder()
                            .username(refreshToken.getUsername())
                            .userType(userData.getUserType())
                            .userRole(userData.getUserRole())
                            .citrixCsrfToken((String) claims.get("citrixCsrfToken"))
                            .citrixSessionId((String) claims.get("citrixSessionId"))
                            .citrixAuthId((String) claims.get("citrixAuthId"))
                            .build();

                    String newAccessToken = tokenProvider.generateToken(authUser);
                    refreshToken.updateAccessToken(newAccessToken);
                    refreshTokenRepository.save(refreshToken);
                    log.info("Successfully refreshed session for user: {}", username);

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
