package com.hmws.citrix.storefront.session_mgmt.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LoginFormDto {

    private String postBackUrl;
    private String cancelUrl;
    private List<RequirementDto> requirements;

}
