package com.hmws.usermgmt.domain;

import com.hmws.password.domain.PasswordLogs;
import com.hmws.portal_log_in_records.domain.PortalLogInRecord;
import com.hmws.usermgmt.constant.UserRole;
import com.hmws.usermgmt.constant.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password", "portalLogInRecord"})
public class UserData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long userNumber;

    @Column(nullable = false, length = 20)
    private String userId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "passwordId")
    private PasswordLogs userPasswordLogs;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 30)
    private String firstName;

    @Column(nullable = false, length = 30)
    private String lastName;

    @Column(nullable = false, length = 20)
    private String telephone;

    @Column(nullable = false, length = 20)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole userRole;

    @Column(nullable = false)
    private String securityGroup;

    @Column(nullable = false)
    private String organizationalUnitPath;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private boolean isDeleted;

    private boolean isActive;

    @OneToMany(mappedBy = "userData", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortalLogInRecord> portalLogInRecord = new ArrayList<>();

    private String region;
}
