package com.hmws.global.authentication.domain;

import com.hmws.usermgmt.domain.UserData;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, length = 1024)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "current_access_token", length = 1024)
    private String currentAccessToken;

    @Builder
    public RefreshToken(String username, String token, LocalDateTime expiryDate, String currentAccessToken) {
        this.username = username;
        this.token = token;
        this.expiryDate = expiryDate;
        this.currentAccessToken = currentAccessToken;
    }

    // 액세스 토큰 업데이트 메서드
    public void updateAccessToken(String newAccessToken) {
        this.currentAccessToken = newAccessToken;
    }
}
