package com.hmws.citrix.storefront.session_mgmt.dto;

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
