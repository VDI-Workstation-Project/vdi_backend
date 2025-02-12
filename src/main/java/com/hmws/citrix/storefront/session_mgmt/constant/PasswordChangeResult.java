package com.hmws.citrix.storefront.session_mgmt.constant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordChangeResult {

    private final boolean success;
    private final String message;
    private final PasswordChangeStatus status;

    public enum PasswordChangeStatus {
        SUCCESS,                // 비밀번호 변경 성공
        OLD_PASSWORD_MISMATCH,  // 현재 비밀번호 불일치
        POLICY_VIOLATION,       // 비밀번호 정책 위반
        STOREFRONT_ERROR,      // StoreFront 서버 오류
        SYSTEM_ERROR,           // 시스템 오류
        UNEXPECTED_ERROR       // 예상치 못한 오류
    }
}
