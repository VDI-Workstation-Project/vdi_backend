package com.hmws.password.domain;

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
@ToString(exclude = "userData")
public class Password {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long passwordId;

    @OneToOne(mappedBy = "userPassword", cascade = CascadeType.ALL)
    private UserData user;

    @Column(nullable = false)
    private String password;

    private LocalDateTime changedAt;

    private LocalDateTime expiresAt;

    public boolean validatePassword(String rawPassword) {
        return this.password.equals(rawPassword);
    }
}
