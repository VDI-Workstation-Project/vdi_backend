package com.hmws.citrix.storefront.session_mgmt.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PasswordChangeRequest {

    private String username;
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
    private String sessionId;
    private String csrfToken;

}
