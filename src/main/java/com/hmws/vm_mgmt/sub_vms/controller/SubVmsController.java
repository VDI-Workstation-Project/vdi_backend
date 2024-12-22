package com.hmws.vm_mgmt.sub_vms.controller;

import com.hmws.vm_mgmt.sub_vms.domain.SubVms;
import com.hmws.vm_mgmt.sub_vms.service.SubVmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SubVmsController {

    private final SubVmsService subVmsService;

    @PostMapping("/createvm")
    @Transactional
    public ResponseEntity<Map<String, Object>> createVm(Authentication authentication) {

        String userId = authentication.getName();

        log.info("VM 생성 요청 - 사용자: {}", authentication.getName());

        try {
            SubVms createdVm = subVmsService.createVm(userId);
            log.info("VM 생성 완료 - VM명: {}, 상태: {}",
                    createdVm.getSubVmName(),
                    createdVm.getSubVmStatus());


            subVmsService.vmRegistration(userId);
            log.info("VM AD 등록 완료 - VM명: {}, OU: {}, SecurityGroup: {}",
                    createdVm.getSubVmName(),
                    createdVm.getUserData().getOrganizationalUnitPath(),
                    createdVm.getUserData().getSecurityGroup());

            subVmsService.setVmSecurity(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "VM 생성 및 AD 등록이 완료되었습니다",
                    "data", Map.of(
                            "vmName", createdVm.getSubVmName(),
                            "status", createdVm.getSubVmStatus()
                    )
            ));

        } catch (Exception e) {
            log.error("VM 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "VM 생성 중 오류가 발생했습니다",
                            "error", e.getMessage()
                    ));
        }

    }
}
