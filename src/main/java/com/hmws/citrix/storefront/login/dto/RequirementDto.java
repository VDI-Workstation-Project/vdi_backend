package com.hmws.citrix.storefront.login.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequirementDto {

    private String id;
    private String type;
    private String label;
    private String initialValue;

}
