package com.hmws.citrix.storefront.resources.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class StoreFrontResourcesResponse {

    private boolean isSubscriptionEnabled;
    private boolean isUnauthenticatedStore;
    private List<StoreFrontResourcesDto> resources;
}
