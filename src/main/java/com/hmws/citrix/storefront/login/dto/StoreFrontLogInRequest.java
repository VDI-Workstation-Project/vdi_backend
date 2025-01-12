package com.hmws.citrix.storefront.login.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreFrontLogInRequest {

    private String username;
    private String password;
    private boolean saveCredentials;
}
