package com.hmws.portal_log_in_records.domain;

import com.hmws.portal_log_in_records.constant.LogInStatus;
import com.hmws.usermgmt.domain.UserData;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalLogInRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_number")
    private UserData userData;

    private LocalDateTime logInTime;

    private LocalDateTime logOutTime;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private boolean isLogInSuccessful;

    @Column(nullable = false)
    private LogInStatus logInStatus;
}
