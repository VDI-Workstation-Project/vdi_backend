package com.hmws.citrix.storefront.session_mgmt.controller;

import com.hmws.citrix.storefront.session_mgmt.dto.PasswordChangeRequest;
import com.hmws.citrix.storefront.session_mgmt.dto.StoreFrontAuthResponse;
import com.hmws.citrix.storefront.session_mgmt.dto.StoreFrontLogInRequest;
import com.hmws.citrix.storefront.session_mgmt.service.StoreFrontLogInService;
import com.hmws.global.authentication.UserDetailsImpl;
import com.hmws.global.authentication.dto.AuthUserDto;
import com.hmws.global.authentication.dto.LogInResponse;
import com.hmws.global.authentication.service.AuthService;
import com.hmws.global.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class StoreFrontSessionController {

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

            // 비밀번호 변경이 필요한 경우
            if ("update-credentials".equals(authResponse.getResult())) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "result", "update-credentials",
//                        "sessionId", authResponse.getSessionId(),
//                        "csrfToken", authResponse.getCsrfToken(),
                        "message", "비밀번호 변경이 필요합니다."
                ));
            }

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

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        AuthUserDto authUser = userDetails.getAuthUser();

        boolean isLogoutSuccessful = storeFrontLogInService.logout(authUser);

        if (isLogoutSuccessful) {
            return ResponseEntity.ok(Map.of("success", true));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false));

    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request) {

        try {
            StoreFrontAuthResponse authResponse = storeFrontLogInService.changePassword(request);

            if ("success".equals(authResponse.getResult())) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "비밀번호가 성공적으로 변경되었습니다."
                ));

            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                authResponse.getErrorMessage() != null ?
                                        authResponse.getErrorMessage() :
                                        "비밀번호 변경에 실패했습니다."
                        ));
            }

        } catch (Exception e) {
            log.error("Password change failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "비밀번호 변경 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }
}
