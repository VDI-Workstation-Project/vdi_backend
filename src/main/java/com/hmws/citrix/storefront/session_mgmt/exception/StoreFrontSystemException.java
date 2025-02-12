package com.hmws.citrix.storefront.session_mgmt.exception;

public class StoreFrontSystemException extends RuntimeException{

    public StoreFrontSystemException(String message) {
        super(message);
    }

    public StoreFrontSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
