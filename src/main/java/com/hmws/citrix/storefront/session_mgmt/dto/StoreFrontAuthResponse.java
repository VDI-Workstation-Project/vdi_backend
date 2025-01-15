package com.hmws.citrix.storefront.session_mgmt.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@JacksonXmlRootElement(localName = "AuthenticationData")
public class StoreFrontAuthResponse {

    private HttpStatus status;
    private String result;
    private String errorMessage;
    private String authType;

    public boolean isSuccess() {
        return "success".equals(result);
    }
}
