package com.hmws.citrix.storefront.session_mgmt.session;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor // Redis 직렬화를 위해 필요
@RedisHash("storefront_pwd_change_session") // Redis 키 prefix 지정
public class StoreFrontSession implements Serializable { // Redis 직렬화를 위해 Serializable 구현

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String csrfToken;
    private String sessionId;
    private String ctxsAuthId;
    private LocalDateTime createdAt;

    public StoreFrontSession(String csrfToken, String sessionId) {
        this.csrfToken = csrfToken;
        this.sessionId = sessionId;
        this.createdAt = LocalDateTime.now();
    }

}
