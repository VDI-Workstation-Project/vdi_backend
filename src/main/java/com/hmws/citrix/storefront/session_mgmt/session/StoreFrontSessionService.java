package com.hmws.citrix.storefront.session_mgmt.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreFrontSessionService {

    @Qualifier("passwordSessionRedisTemplate")
    private final RedisTemplate<String, StoreFrontSession> redisTemplate;
    private static final String SESSION_PREFIX = "citrix:pwd_change:";

    public void saveSession(StoreFrontSession session) {
        String key = SESSION_PREFIX + session.getSessionId();
        redisTemplate.opsForValue().set(key, session);
        log.debug("임시 세션 저장: {}", session.getSessionId());
    }

    public StoreFrontSession getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        StoreFrontSession session = redisTemplate.opsForValue().get(key);
        if (session != null) {
            log.debug("임시 세션 조회: {}", sessionId);
        }
        return session;
    }

    public void removeSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
        log.debug("임시 세션 제거: {}", sessionId);
    }
}
