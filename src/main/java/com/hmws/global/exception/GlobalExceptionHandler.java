package com.hmws.global.exception;

import com.hmws.citrix.storefront.session_mgmt.exception.StoreFrontSystemException;
import com.hmws.personnel_info.domain.PersonnelNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 전역적으로 예의를 처리하기 위한 어노테이션
// @ControllerAdvicd + @ResponseBody
// 주요 기능:
// 전역 예외 처리: 모든 컨트롤러에서 발생하는 예외를 한 곳에서 처리
// 응답 자동 JSON 변환: @ResponseBody 기능이 포함되어 있어 응답을 자동으로 JSON으로 변환
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PersonnelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePersonnelNotFoundException(PersonnelNotFoundException e) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "서버 내부 오류가 발생했습니다"
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @ExceptionHandler(StoreFrontSystemException.class)
    public ResponseEntity<ErrorResponse> handleStoreFrontSystemException(StoreFrontSystemException e) {
        log.error("StoreFront 시스템 오류", e);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                e.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
