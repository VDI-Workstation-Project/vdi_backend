package com.hmws.usermgmt.dto;

import com.hmws.usermgmt.constant.UserRole;
import com.hmws.usermgmt.constant.UserType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDataDto {

    private Long userNumber;

    @NotNull(message = "ID는 필수 입력 정보입니다")
    private String userId;

    @NotNull(message = "비밀번호는 필수 입력 정보입니다")
    private Long passwordId;

    @NotNull(message = "이메일은 필수 입력 정보입니다")
    private String email;

    @NotNull(message = "이름은 필수 입력 정보입니다")
    private String firstName;

    @NotNull(message = "성은 필수 입력 정보입니다")
    private String lastName;

    @NotNull(message = "전화번호는 필수입니다")
    private String telephone;

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    private String mobile;

    @NotNull(message = "사용자 유형은 필수 입력 정보입니다")
    private UserType userType;

    @NotNull(message = "직급은 필수 입력 정보입니다")
    private UserRole userRole;

    private String SecurityGroup;

    private String OrganizationUnitPath;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private boolean isDeleted;

    private boolean isActive;

    private Long portalLogInRecordId;

    private String region;
}
