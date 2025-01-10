package com.hmws.global.authentication.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthUserDto {

    private final String username;
    private final String citrixCsrfToken;
    private final String citrixSessionId;
    private final String citrixAuthId;

}
