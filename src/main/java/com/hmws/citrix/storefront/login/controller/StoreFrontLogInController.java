package com.hmws.citrix.storefront.login.controller;

import com.hmws.citrix.storefront.login.dto.StoreFrontAuthResponse;
import com.hmws.citrix.storefront.login.dto.StoreFrontLogInRequest;
import com.hmws.citrix.storefront.login.service.StoreFrontLogInService;
import com.hmws.global.authentication.dto.LogInResponse;
import com.hmws.global.authentication.service.AuthService;
import com.hmws.global.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class StoreFrontLogInController {

    private final StoreFrontLogInService storeFrontLogInService;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody StoreFrontLogInRequest request) {

        try {
            StoreFrontAuthResponse authResponse = storeFrontLogInService.storeFrontLogin(
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

            LogInResponse response = authService.getTokens(request);

            return ResponseEntity.ok(response);

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
