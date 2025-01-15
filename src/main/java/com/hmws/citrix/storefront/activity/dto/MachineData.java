package com.hmws.citrix.storefront.activity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MachineData {

    private String machineId;
    private String machineName;
    private String powerState;
    private String registrationState;
    private String sessionSupport;
    private boolean shutDownSupported;
    private boolean turnOffSupported;
}
