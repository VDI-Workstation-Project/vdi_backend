package com.hmws.global.authentication.controller;

import com.hmws.citrix.storefront.session_mgmt.service.StoreFrontLogInService;
import com.hmws.global.authentication.dto.RedisRefreshToken;
import com.hmws.global.authentication.repository.RedisRefreshTokenRepository;
import com.hmws.global.authentication.utils.TokenProvider;
import com.hmws.global.exception.ErrorResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AuthController {

    private final TokenProvider tokenProvider;
    private final RedisRefreshTokenRepository refreshTokenRepository;
    private final StoreFrontLogInService storeFrontLogInService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        try {
            if (!tokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(401, "Invalid refresh Token"));
            }

            RedisRefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh Token Not Found"));

            String newAccessToken = tokenProvider.refreshAccessToken(storedToken.getUsername());

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(401, "Failed to refresh Token"));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String token = tokenProvider.resolveToken(request);

        if (token != null && tokenProvider.validateToken(token)) {
            Claims claims = tokenProvider.getClaims(token);

            boolean sessionValid = storeFrontLogInService.keepAliveSession(
                    (String) claims.get("citrixSessionId"),
                    (String) claims.get("citrixCsrfToken")
            );

            if (sessionValid) {
                return ResponseEntity.ok(Map.of("success", true));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false));
    }
}
