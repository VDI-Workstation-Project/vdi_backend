package com.hmws.global.authentication;

import com.hmws.citrix.storefront.service.StoreFrontService;
import com.hmws.citrix.storefront.session.CitrixSession;
import com.hmws.global.authentication.domain.RefreshToken;
import com.hmws.global.authentication.dto.AuthUserDto;
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

    private final StoreFrontService storeFrontService;
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-time}")
    private long tokenExpirationTime;

    @Value("${jwt.refresh-token.expiration-time}")
    private long refreshTokenExpirationTime;

    private Key key;

    private final RefreshTokenRepository refreshTokenRepository;

    @PostConstruct
    protected void init() {
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
        key = Keys.hmacShaKeyFor(encodedKey.getBytes());
    }

    public String generateToken(AuthUserDto authUser) {
        return Jwts.builder()
                .setSubject(authUser.getUsername())
                .setHeader(createHeader())
                .setClaims(createClaims(authUser))
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

    public Map<String, Object> createClaims(AuthUserDto authUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", authUser.getUsername());
        claims.put("citrixCsrfToken", authUser.getCitrixCsrfToken());
        claims.put("citrixSessionId", authUser.getCitrixSessionId());
        claims.put("citrixAuthId", authUser.getCitrixAuthId());

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

        Claims claims = getClaims(token);
        String username = (String) claims.get("username");

        AuthUserDto authUser = AuthUserDto.builder()
                .username(username)
                .citrixCsrfToken((String) claims.get("citrixCsrfToken"))
                .citrixSessionId((String) claims.get("citrixSessionId"))
                .citrixAuthId((String) claims.get("citrixAuthId"))
                .build();

        UserDetails userDetails = new UserDetailsImpl(authUser);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String generateRefreshToken(String username) {

        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshTokenExpirationTime);

        String refreshToken = Jwts.builder()
                .setSubject(username)
                .setExpiration(java.sql.Timestamp.valueOf(expiryDate))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .username(username)
                .token(refreshToken)
                .expiryDate(expiryDate)
                .build();

        refreshTokenRepository.findByUsername(username).ifPresent(token -> refreshTokenRepository.deleteByUsername(username));

        refreshTokenRepository.save(refreshTokenEntity);

        return refreshToken;
    }

    public String refreshAccessToken(String username) {

        CitrixSession citrixSession = storeFrontService.getCurrentSession();

        AuthUserDto authUser = AuthUserDto.builder()
                .username(citrixSession.getCsrfToken())
                .citrixCsrfToken(citrixSession.getCsrfToken())
                .citrixSessionId(citrixSession.getSessionId())
                .citrixAuthId(citrixSession.getCtxsAuthId())
                .build();

        return generateToken(authUser);
    }
}
