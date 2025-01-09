package com.hmws.citrix.storefront.controller;

import com.hmws.citrix.storefront.dto.StoreFrontLogInRequest;
import com.hmws.citrix.storefront.service.StoreFrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/citrix")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class StoreFrontController {

    private final StoreFrontService storeFrontService;

    @PostMapping("/test-connection")
    public ResponseEntity<?> testConnection() {

        log.info("test-connection controller entrance");

        return ResponseEntity.ok(storeFrontService.getStoreFrontLoginForm());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody StoreFrontLogInRequest request) {
        return ResponseEntity.ok(storeFrontService.authenticateUser(
                request.getUsername(),
                request.getPassword(),
                request.isSaveCredentials()
        ));
    }

}
