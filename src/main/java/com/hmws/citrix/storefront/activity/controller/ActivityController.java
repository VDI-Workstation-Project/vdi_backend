package com.hmws.citrix.storefront.activity.controller;

import com.hmws.citrix.storefront.activity.service.ActivityService;
import com.hmws.global.authentication.UserDetailsImpl;
import com.hmws.global.authentication.dto.AuthUserDto;
import com.hmws.global.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storefront/activity")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Slf4j
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions() {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            AuthUserDto authUser = userDetails.getAuthUser();

            return ResponseEntity.ok(activityService.getActiveSessions(authUser));

        } catch (Exception e) {
            log.error("Failed to get resources", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "An error occurred while retrieving the session: " + e.getMessage()));
        }
    }

    @GetMapping("/machine/power-state/{machineId}")
    public ResponseEntity<?> checkMachinePowerState(@PathVariable String machineId) {

        log.info("checkMachinePowerState entrance");

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            AuthUserDto authUser = userDetails.getAuthUser();

            String powerState = activityService.checkMachinePowerStateWithPolling(authUser, machineId);
            return ResponseEntity.ok().body(Map.of("powerState", powerState));

        } catch (Exception e) {
            log.error("Failed to check machine state", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "Failed to check machine state: " + e.getMessage()));
        }
    }


    @PostMapping("/machine/shutdown/{machineId}")
    public ResponseEntity<?> shutdownMachine(@PathVariable String machineId) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            AuthUserDto authUser = userDetails.getAuthUser();

            activityService.shutdownMachine(authUser, machineId);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to shutting down the machine", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "An error occurred while shutting down the machine: " + e.getMessage()));
        }
    }


}
