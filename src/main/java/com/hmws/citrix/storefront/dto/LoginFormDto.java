package com.hmws.citrix.storefront.dto;

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
