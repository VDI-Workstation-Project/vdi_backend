package com.hmws.citrix.storefront.controller;

import com.hmws.citrix.storefront.dto.StoreFrontAuthResponse;
import com.hmws.citrix.storefront.dto.StoreFrontLogInRequest;
import com.hmws.citrix.storefront.service.StoreFrontService;
import com.hmws.global.authentication.TokenProvider;
import com.hmws.global.authentication.dto.AuthUserDto;
import com.hmws.global.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/citrix")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class StoreFrontController {

    private final StoreFrontService storeFrontService;
    private final TokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody StoreFrontLogInRequest request) {

        try {
            StoreFrontAuthResponse authResponse = storeFrontService.storeFrontLogin(
                    request.getUsername(),
                    request.getPassword(),
                    request.isSaveCredentials()
            );

            // 로그인 실패 시
            if (!authResponse.isSuccess()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                authResponse.getErrorMessage() != null ?
                                        authResponse.getErrorMessage() : "로그인에 실패했습니다."
                        ));
            }

            // 로그인 성공 시 JWT 토큰 생성
            AuthUserDto authUser = AuthUserDto.builder()
                    .username(request.getUsername())
                    .citrixCsrfToken(storeFrontService.getCurrentSession().getCsrfToken())
                    .citrixSessionId(storeFrontService.getCurrentSession().getSessionId())
                    .citrixAuthId(storeFrontService.getCurrentSession().getCtxsAuthId())
                    .build();

            String accessToken = tokenProvider.generateToken(authUser);
            String refreshToken = tokenProvider.generateRefreshToken(request.getUsername());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "citrixSession", Map.of(
                            "csrfToken", authUser.getCitrixCsrfToken(),
                            "sessionId", authUser.getCitrixSessionId(),
                            "authId", authUser.getCitrixAuthId()
                    )
            ));

        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "로그인 처리 중 오류가 발생했습니다: " + e.getMessage()
                    ));

        }


    }

}
