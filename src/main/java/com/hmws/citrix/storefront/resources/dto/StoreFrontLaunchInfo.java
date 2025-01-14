package com.hmws.citrix.storefront.resources.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreFrontLaunchInfo {

    private String ctxsDeviceId;
    private String launchStatusUrl;
    private String launchUrl;
    private String desktopHostname;
    private String resourceId;
    private LocalDateTime createdAt;
}
