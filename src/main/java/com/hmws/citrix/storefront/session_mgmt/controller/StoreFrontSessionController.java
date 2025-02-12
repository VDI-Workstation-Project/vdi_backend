package com.hmws.citrix.storefront.session_mgmt.controller;

import com.hmws.citrix.storefront.session_mgmt.constant.PasswordChangeResult;
import com.hmws.citrix.storefront.session_mgmt.dto.PasswordChangeRequest;
import com.hmws.citrix.storefront.session_mgmt.dto.StoreFrontAuthResponse;
import com.hmws.citrix.storefront.session_mgmt.dto.StoreFrontLogInRequest;
import com.hmws.citrix.storefront.session_mgmt.service.PasswordChangeService;
import com.hmws.citrix.storefront.session_mgmt.service.StoreFrontLogInService;
import com.hmws.citrix.storefront.session_mgmt.session.StoreFrontSessionService;
import com.hmws.global.authentication.utils.UserDetailsImpl;
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
    private final PasswordChangeService passwordChangeService;
    private final StoreFrontSessionService sessionService;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody StoreFrontLogInRequest request) {

        try {
            StoreFrontAuthResponse authResponse = storeFrontLogInService.storeFrontLogin(
                    request.getUsername(),
                    request.getPassword(),
                    request.isSaveCredentials()
            );

            log.info("authResponse result {}", authResponse.getResult());
            log.info("authResponse message {}", authResponse.getMessage());

            // 아이디 혹은 비밀번호가 틀렸을 경우
            if ("error".equals(authResponse.getResult()) && "Incorrect user name or password".equals(authResponse.getMessage())) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "result", "Incorrect user name or password",
                        "message", "잘못된 사용자 이름 또는 비밀번호입니다."
                ));
            }

            // 비밀번호 변경이 필요한 경우
            if ("update-credentials".equals(authResponse.getResult())) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "result", "update-credentials",
                        "sessionId", authResponse.getSessionId(),
                        "message", "비밀번호 변경이 필요합니다."
                ));
            }

            // 로그인 실패 시
            if (!authResponse.isSuccess()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                authResponse.getMessage() != null ?
                                        authResponse.getMessage() : "로그인에 실패했습니다."
                        ));
            }

            LogInResponse response = authService.getTokens(request, authResponse);

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

            if (sessionService.getSession(request.getSessionId()) == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                "No active session found"
                        ));
            }

            PasswordChangeResult result = passwordChangeService.changePassword(request);

            return ResponseEntity.ok(result);

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
