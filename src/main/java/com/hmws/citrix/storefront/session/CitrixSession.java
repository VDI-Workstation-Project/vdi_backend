package com.hmws.citrix.storefront.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class CitrixSession {

    private String csrfToken;
    private String sessionId;
    private String ctxsAuthId;

    public CitrixSession(String csrfToken, String sessionId) {
        this.csrfToken = csrfToken;
        this.sessionId = sessionId;
    }

}
