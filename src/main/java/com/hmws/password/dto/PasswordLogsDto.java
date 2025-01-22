package com.hmws.password.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordLogsDto {

    private Long passwordId;

    private Long userNumber;

    private LocalDateTime changedAt;

    private LocalDateTime expiresAt;
}
