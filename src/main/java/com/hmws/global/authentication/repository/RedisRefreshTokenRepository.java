package com.hmws.global.authentication.repository;

import com.hmws.global.authentication.dto.RedisRefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisRefreshTokenRepository {

    private final RedisTemplate<String, RedisRefreshToken> redisTemplate;
    private static final String KEY_PREFIX = "refresh_token:";

    public void save(RedisRefreshToken refreshToken) {
        String key = KEY_PREFIX + refreshToken.getUsername();
        Duration ttl = Duration.between(LocalDateTime.now(), refreshToken.getExpiryDate());
        redisTemplate.opsForValue().set(key, refreshToken, ttl);
        log.debug("Refresh token saved for user: {}", refreshToken.getUsername());
    }

    public Set<RedisRefreshToken> findAll() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return new HashSet<>();
        }

        return keys.stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Optional<RedisRefreshToken> findByUsername(String username) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + username));
    }

    public Optional<RedisRefreshToken> findByToken(String token) {
        return redisTemplate.keys(KEY_PREFIX + "*")
                .stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(refreshToken -> refreshToken.getToken().equals(token))
                .findFirst();
    }

    public void deleteByUsername(String username) {
        redisTemplate.delete(KEY_PREFIX + username);
        log.debug("Refresh token deleted for user: {}", username);
    }
}
