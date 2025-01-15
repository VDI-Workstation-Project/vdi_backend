package com.hmws.citrix.storefront.activity.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ActivityResponse {

    private String deviceId;
    private List<ActivitySession> sessions;
}
