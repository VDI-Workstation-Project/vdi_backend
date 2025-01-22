package com.hmws.global.authentication.dto;

import com.hmws.usermgmt.constant.UserRole;
import com.hmws.usermgmt.constant.UserType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthUserDto {

    private final String username;
    private final UserType userType;
    private final UserRole userRole;
    private final String citrixCsrfToken;
    private final String citrixSessionId;
    private final String citrixAuthId;

}
