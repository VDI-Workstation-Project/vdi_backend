package com.hmws.usermgmt.controller;

import com.hmws.global.authentication.TokenProvider;
import com.hmws.personnel_info.dto.UserRegistrationRequestDto;
import com.hmws.portal_log_in_records.domain.PortalLogInRecord;
import com.hmws.portal_log_in_records.dto.PortalLoginRecordDto;
import com.hmws.usermgmt.domain.UserData;
import com.hmws.usermgmt.dto.LogInRequestDto;
import com.hmws.usermgmt.dto.UserDataDto;
import com.hmws.usermgmt.service.UserDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserDataController {

    private final UserDataService userDataService;

    private  final TokenProvider tokenProvider;

    private final AuthenticationManagerBuilder authMgmtBuilder;


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody String refreshToken) {
        try {
            String newAccessToken = tokenProvider.refreshAcessTokoen(refreshToken);
            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("토큰 갱신에 실패했습니다: " + e.getMessage());
        }
    }



    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LogInRequestDto request) {

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword());

        Authentication authentication = authMgmtBuilder.getObject().authenticate(authToken);
        boolean isAuthenticated = authentication.isAuthenticated();

        String accessToken = "";
        String refreshToken = "";

        try {
            if (isAuthenticated) {

                LocalDateTime logInAttemptTime = LocalDateTime.parse(request.getLogInAttemptTime(), DateTimeFormatter.ISO_DATE_TIME);

                UserDataDto userData = userDataService.getUserByUserId(request.getUserId(), logInAttemptTime);

                accessToken = tokenProvider.generateToken(userData);
                refreshToken = tokenProvider.generateRefreshToken(userData.getUserId());

            }
            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    // ResponseEntity: HTTP 응답을 표현하는 클래스
    // @Valid: 요청 데이터의 유효성을 검사하는 어노테이션. UserRegistrationRequest 클래스에 정의된 제약조건을 검사
    // RequestBody: HTTP 요청의 본문(JSON)을 Java 객체로 변환해주는 어노테이션. 클라이언트가 보낸 JSON 데이터를 UserRegistrationRequest 객체로 자동 변환
    @PostMapping("/createAccount")
    public ResponseEntity<String> createAccount(@Valid @RequestBody UserRegistrationRequestDto request) {
        userDataService.createAdUser(request);
        return ResponseEntity.ok("AD 사용자 생성 요청이 완료되었습니다.");
    }
}
