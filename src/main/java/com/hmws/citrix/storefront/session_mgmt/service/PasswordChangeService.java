package com.hmws.citrix.storefront.session_mgmt.service;

import com.hmws.citrix.storefront.session_mgmt.constant.PasswordChangeResult;
import com.hmws.citrix.storefront.session_mgmt.dto.PasswordChangeRequest;
import com.hmws.citrix.storefront.session_mgmt.dto.StoreFrontAuthResponse;
import com.hmws.citrix.storefront.session_mgmt.exception.StoreFrontSystemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordChangeService {

    private final StoreFrontLogInService storeFrontLogInService;

    public PasswordChangeResult changePassword(PasswordChangeRequest request) {

        try {
            StoreFrontAuthResponse authResponse = storeFrontLogInService.changePassword(request);

            return createPasswordChangeResult(authResponse);

        } catch (Exception e) {
            log.error("비밀번호 변경 중 시스템 오류 발생", e);
            throw new StoreFrontSystemException("비밀번호 변경 처리 중 시스템 오류가 발생했습니다.", e);
        }
    }

    private PasswordChangeResult createPasswordChangeResult(StoreFrontAuthResponse authResponse) {
        if (authResponse.isSuccess()) {
            return createSuccessResult();
        }
        return createErrorResult(authResponse);
    }

    private PasswordChangeResult createSuccessResult() {
        return PasswordChangeResult.builder()
                .success(true)
                .status(PasswordChangeResult.PasswordChangeStatus.SUCCESS)
                .message("비밀번호가 성공적으로 변경되었습니다.")
                .build();
    }

    private PasswordChangeResult createErrorResult(StoreFrontAuthResponse authResponse) {

        String message = authResponse.getMessage();
        String result = authResponse.getResult();

        if ("Old password incorrect".equals(message)) {
            return createOldPasswordMismatchResult();
        }

        if (message != null && message.contains("password policy requirements")) {
            return createPolicyViolationResult();
        }

        if ("fail".equals(result)) {
            return createSystemErrorResult(message);
        }

        return createUnexpectedErrorResult(message);
    }

    private PasswordChangeResult createOldPasswordMismatchResult() {
        return PasswordChangeResult.builder()
                .success(false)
                .status(PasswordChangeResult.PasswordChangeStatus.OLD_PASSWORD_MISMATCH)
                .message("현재 비밀번호가 일치하지 않습니다.")
                .build();
    }

    private PasswordChangeResult createPolicyViolationResult() {
        return PasswordChangeResult.builder()
                .success(false)
                .status(PasswordChangeResult.PasswordChangeStatus.POLICY_VIOLATION)
                .message("비밀번호 정책을 만족하지 않습니다.")
                .build();
    }

    private PasswordChangeResult createSystemErrorResult(String message) {

        String errorMessage;
        PasswordChangeResult.PasswordChangeStatus status;

        if ("fatalerror".equals(message)) {
            errorMessage = "StoreFront 서버와 통신 중 오류가 발생했습니다. 새로고침 후 다시 시도해 주세요.";
            status = PasswordChangeResult.PasswordChangeStatus.STOREFRONT_ERROR;
        } else {
            errorMessage = "비밀번호 변경 처리 중 오류가 발생했습니다.";
            status = PasswordChangeResult.PasswordChangeStatus.SYSTEM_ERROR;
        }

        log.error("StoreFront 시스템 오류 발생: {}", message);

        return PasswordChangeResult.builder()
                .success(false)
                .status(status)
                .message(errorMessage)
                .build();
    }

    private PasswordChangeResult createUnexpectedErrorResult(String message) {
        log.error("예상치 못한 오류 발생: {}", message);

        return PasswordChangeResult.builder()
                .success(false)
                .status(PasswordChangeResult.PasswordChangeStatus.UNEXPECTED_ERROR)
                .message("알 수 없는 오류가 발생했습니다. 관리자에게 문의해 주세요.")
                .build();
    }
}
