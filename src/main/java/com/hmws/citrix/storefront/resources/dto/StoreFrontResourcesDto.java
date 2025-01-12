package com.hmws.citrix.storefront.resources.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class StoreFrontResourcesDto {

    private List<String> clienttypes;
    private String desktopassignmenttype;
    private String desktophostname;
    private String iconurl;
    private String id;
    private boolean isdesktop;
    private String launchstatusurl;
    private String launchurl;
    private String name;
    private String path;
    private String poweroffurl;
    private String shortcutvalidationurl;
    private String subscriptionurl;

}
