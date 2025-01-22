package com.hmws.password.domain;

import com.hmws.usermgmt.domain.UserData;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "userData")
public class PasswordLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long passwordId;

    @OneToOne(mappedBy = "userPasswordLogs", cascade = CascadeType.ALL)
    private UserData user;

    private LocalDateTime changedAt;

    private LocalDateTime expiresAt;

    public void logPasswordChange(LocalDateTime changedAt, LocalDateTime expiresAt) {
        this.changedAt = changedAt;
        this.expiresAt = expiresAt;
    }
}
