package com.hmws.password.dto;

import com.hmws.usermgmt.domain.UserData;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordDto {

    private Long passwordId;

    private Long userNumber;

    @NotNull(message = "비밀번호는 필수 입력 정보입니다")
    private String password;

    private LocalDateTime changedAt;

    private LocalDateTime expiresAt;
}
