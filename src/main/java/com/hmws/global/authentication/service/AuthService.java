package com.hmws.global.authentication.service;

import com.hmws.citrix.storefront.session_mgmt.dto.StoreFrontAuthResponse;
import com.hmws.citrix.storefront.session_mgmt.dto.StoreFrontLogInRequest;
import com.hmws.global.authentication.dto.RedisRefreshToken;
import com.hmws.global.authentication.repository.RedisRefreshTokenRepository;
import com.hmws.global.authentication.utils.TokenProvider;
import com.hmws.global.authentication.dto.AuthUserDto;
import com.hmws.global.authentication.dto.LogInResponse;
import com.hmws.usermgmt.domain.UserData;
import com.hmws.usermgmt.repository.UserDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final TokenProvider tokenProvider;
    private final RedisRefreshTokenRepository refreshTokenRepository;
    private final UserDataRepository userRepository;

    @Value("${jwt.refresh-token.expiration-time}")
    private long refreshTokenExpirationTime;

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
        refreshTokenRepository.deleteByUsername(request.getUsername());

        saveRefreshToken(request.getUsername(), refreshToken, accessToken);

        return new LogInResponse(true, accessToken, refreshToken);
    }

    private void saveRefreshToken(String username, String refreshToken, String accessToken) {

        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshTokenExpirationTime);

        // RefreshToken 엔티티 생성 및 저장
        RedisRefreshToken redisToken = RedisRefreshToken.builder()
                .username(username)
                .token(refreshToken)
                .currentAccessToken(accessToken)
                .expiryDate(expiryDate)
                .build();

        refreshTokenRepository.save(redisToken);
    }

}
