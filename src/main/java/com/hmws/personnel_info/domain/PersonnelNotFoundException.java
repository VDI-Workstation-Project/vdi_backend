package com.hmws.personnel_info.domain;

import org.springframework.stereotype.Component;

public class PersonnelNotFoundException extends RuntimeException{

    public PersonnelNotFoundException(String message) {
        super(message);
    }

    public PersonnelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
