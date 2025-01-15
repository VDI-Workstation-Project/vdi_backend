package com.hmws.citrix.storefront.activity.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ActivitySession {

    private List<Application> applications;
    private String clientName;
    private String clientType;
    private String connectionState;
    private String deviceId;
    private String deviceType;
    private MachineData machineData;
    private String sessionId;
    private String sessionStartTime;
    private String userSessionType;
}
