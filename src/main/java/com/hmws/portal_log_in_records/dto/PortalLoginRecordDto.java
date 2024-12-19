package com.hmws.portal_log_in_records.dto;

import com.hmws.usermgmt.domain.UserData;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PortalLoginRecordDto {

    private Long recordId;

    private LocalDateTime logInTime;

    private LocalDateTime logOutTime;

    private String ipAddress;

    private boolean isLogInSuccessful;

    private String logInStatus;
}
