package com.hmws.citrix.storefront.resources.repository;

import com.hmws.citrix.storefront.resources.dto.StoreFrontLaunchInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class StoreFrontLaunchInfoRepository {

    private final RedisTemplate<String, StoreFrontLaunchInfo> redisTemplate;
    private static final String KEY_PREFIX = "storefront:launch:";

    public void save(String username, StoreFrontLaunchInfo launchInfo) {

        String key = KEY_PREFIX + username;
        redisTemplate.opsForValue().set(key, launchInfo);
        redisTemplate.expire(key, Duration.ofHours(1));

    }

    public StoreFrontLaunchInfo findByUsername(String username) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + username);
    }
}
