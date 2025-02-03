package com.hmws.global.authentication.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisRefreshToken implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String token;
    private LocalDateTime expiryDate;
    private String currentAccessToken;
}
