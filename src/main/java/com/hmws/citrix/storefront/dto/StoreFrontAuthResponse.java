package com.hmws.citrix.storefront.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JacksonXmlRootElement(localName = "AuthenticationData")
public class StoreFrontAuthResponse {

    private String status;
    private String result;
    private String errorMessage;

    public boolean isSuccess() {
        return "success".equals(status);
    }
}
