package com.hmws.global.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LogInResponse {

    private boolean success;
    private String accessToken;
    private String refreshToken;

}
