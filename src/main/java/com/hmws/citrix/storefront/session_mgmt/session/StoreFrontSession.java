package com.hmws.citrix.storefront.session_mgmt.session;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoreFrontSession {

    private String csrfToken;
    private String sessionId;
    private String ctxsAuthId;

    public StoreFrontSession(String csrfToken, String sessionId) {
        this.csrfToken = csrfToken;
        this.sessionId = sessionId;
    }

}
