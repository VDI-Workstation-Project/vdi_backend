package com.hmws.personnel_info.dto;

import com.hmws.usermgmt.constant.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonnelInfoDto {

    private Long personnelId;

    @NotBlank(message = "이름은 필수 입력 정보입니다")
    private String firstName;

    @NotBlank(message = "성은 필수 입력 정보입니다")
    private String lastName;

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    private String phoneNumber;

    @NotNull(message = "직급은 필수 입력 정보입니다")
    private UserRole role;

    @NotNull(message = "부서는 필수 입력 정보입니다")
    private String department;
}
