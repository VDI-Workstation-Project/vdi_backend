package com.hmws.global.authentication;

import com.hmws.global.authentication.domain.RefreshToken;
import com.hmws.global.authentication.repository.RefreshTokenRepository;
import com.hmws.usermgmt.dto.UserDataDto;
import com.hmws.usermgmt.service.UserDataService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-time}")
    private long tokenExpirationTime;

    @Value("${jwt.refresh-token.expiration-time}")
    private long refreshTokenExpirationTime;

    private Key key;

    private final UserDetailsService userDetailsService;

    private final UserDataService userDataService;

    private final RefreshTokenRepository refreshTokenRepository;

    @PostConstruct
    protected void init() {
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
        key = Keys.hmacShaKeyFor(encodedKey.getBytes());
    }

    public String generateToken(UserDataDto userDataDto) {
        return Jwts.builder()
                .setSubject(userDataDto.getUserId())
                .setHeader(createHeader())
                .setClaims(createClaims(userDataDto))
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Map<String, Object> createHeader() {
        Map<String, Object> header = new HashMap<>();
        header.put("typ", "JWT");
        header.put("algorithm", "HS256");
        header.put("regDate", System.currentTimeMillis() / 1000);

        return header;
    }

    public Map<String, Object> createClaims(UserDataDto userDataDto) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDataDto.getUserId());

        return claims;
    }

    public Claims getClaims(String token) {
        return (Claims) Jwts.parserBuilder().setSigningKey(key).build().parse(token).getBody();
    }

    public String getUserId(String token) {
        return (String) getClaims(token).get("userId");
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(getUserId(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String generateRefreshToken(String userId) {

        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshTokenExpirationTime);

        String refreshToken = Jwts.builder()
                .setSubject(userId)
                .setExpiration(java.sql.Timestamp.valueOf(expiryDate))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(userId)
                .token(refreshToken)
                .expiryDate(expiryDate)
                .build();

        refreshTokenRepository.findByUserId(userId).ifPresent(token -> refreshTokenRepository.deleteByUserId(userId));

        refreshTokenRepository.save(refreshTokenEntity);

        return refreshToken;
    }

    public String refreshAcessTokoen(String refreshToken) {

        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token이 존재하지 않습니다"));

        if (savedRefreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(savedRefreshToken);
            throw new RuntimeException("Refresh Token이 만료되었습니다");
        }

        UserDataDto userDataDto = userDataService.getUserByUserId(savedRefreshToken.getUserId(), LocalDateTime.now());
        return generateToken(userDataDto);
    }
}
