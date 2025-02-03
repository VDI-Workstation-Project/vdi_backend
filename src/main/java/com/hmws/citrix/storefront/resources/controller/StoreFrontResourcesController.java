package com.hmws.citrix.storefront.resources.controller;

import com.hmws.citrix.storefront.resources.dto.StoreFrontResourcesDto;
import com.hmws.citrix.storefront.resources.service.StoreFrontResourcesService;
import com.hmws.global.authentication.utils.UserDetailsImpl;
import com.hmws.global.authentication.dto.AuthUserDto;
import com.hmws.global.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storefront/resources")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class StoreFrontResourcesController {

    private final StoreFrontResourcesService storeFrontResourcesService;

    @PostMapping("/list")
    public ResponseEntity<?> getResources() {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            AuthUserDto authUser = userDetails.getAuthUser();

            List<StoreFrontResourcesDto> resources = storeFrontResourcesService.getResources(authUser);
            return ResponseEntity.ok(resources);

        } catch (Exception e) {
            log.error("Failed to get resources", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "리소스 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/{resourceId}/launch/status")
    public ResponseEntity<?> getLaunchStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            AuthUserDto authUser = userDetails.getAuthUser();

            return ResponseEntity.ok(storeFrontResourcesService.checkLaunchStatus(authUser));

        } catch (Exception e) {
            log.error("Launch status check failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "VM status check failed: " + e.getMessage()));
        }

    }

    @GetMapping("/{resourceId}/launch/ica")
    public ResponseEntity<?> getLaunchIca() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            AuthUserDto authUser = userDetails.getAuthUser();

            byte[] icaFile = storeFrontResourcesService.getLaunchIca(authUser);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=launch.ica")
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-ica")
                    .body(icaFile);

        } catch (Exception e) {
            log.error("ICA file retrieval failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "ICA creation failed: " + e.getMessage()));
        }
    }
}
