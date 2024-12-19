package com.hmws.personnel_info.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequestDto {

    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotBlank(message = "이름은 필수입니다")
    private String firstName;

    @NotBlank(message = "성은 필수입니다")
    private String lastName;

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    private String phoneNumber;


}
