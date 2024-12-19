package com.hmws.usermgmt.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogInRequestDto {

    private String userId;

    private String email;

    private String password;

    private String logInAttemptTime;

}
