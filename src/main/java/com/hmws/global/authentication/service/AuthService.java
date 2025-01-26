package com.hmws.global.authentication.service;

import com.hmws.citrix.storefront.session_mgmt.dto.StoreFrontAuthResponse;
import com.hmws.citrix.storefront.session_mgmt.dto.StoreFrontLogInRequest;
import com.hmws.citrix.storefront.session_mgmt.service.StoreFrontLogInService;
import com.hmws.global.authentication.TokenProvider;
import com.hmws.global.authentication.domain.RefreshToken;
import com.hmws.global.authentication.dto.AuthUserDto;
import com.hmws.global.authentication.dto.LogInResponse;
import com.hmws.global.authentication.repository.RefreshTokenRepository;
import com.hmws.usermgmt.domain.UserData;
import com.hmws.usermgmt.repository.UserDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDataRepository userRepository;

    public LogInResponse getTokens(StoreFrontLogInRequest request, StoreFrontAuthResponse storeFrontResponse) {

        log.info("AuthService Username: {}", request.getUsername());

        UserData userData = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 로그인 성공 시 JWT 토큰 생성
        AuthUserDto authUser = AuthUserDto.builder()
                .username(request.getUsername())
                .userType(userData.getUserType())
                .userRole(userData.getUserRole())
                .citrixCsrfToken(storeFrontResponse.getCsrfToken())
                .citrixSessionId(storeFrontResponse.getSessionId())
                .citrixAuthId(storeFrontResponse.getCtxsAuthId())
                .build();

        String accessToken = tokenProvider.generateToken(authUser);
        String refreshToken = tokenProvider.generateRefreshToken(request.getUsername());

        log.info("Access token: {}", accessToken);
        log.info("Refresh token: {}", refreshToken);

        // 기존 토큰이 있다면 삭제
        refreshTokenRepository.findByUsername(request.getUsername())
                .ifPresent(token -> refreshTokenRepository.delete(token));

        saveRefreshToken(request.getUsername(), refreshToken, accessToken);

        return new LogInResponse(true, accessToken, refreshToken);
    }

    private void saveRefreshToken(String username, String refreshToken, String accessToken) {

        // RefreshToken 엔티티 생성 및 저장
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .username(username)
                .token(refreshToken)
                .currentAccessToken(accessToken)  // 현재 액세스 토큰 설정
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

    }

}
