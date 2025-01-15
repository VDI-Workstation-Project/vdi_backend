package com.hmws.citrix.storefront.activity.service;

import com.hmws.citrix.storefront.activity.dto.ActivityResponse;
import com.hmws.citrix.storefront.activity.dto.ActivitySession;
import com.hmws.citrix.storefront.resources.repository.StoreFrontLaunchInfoRepository;
import com.hmws.citrix.storefront.resources.service.StoreFrontResourcesService;
import com.hmws.global.authentication.dto.AuthUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private static final String BASE_URL = "http://172.24.247.151/Citrix/hmstoreWeb";
    private final RestTemplate restTemplate;
    private final StoreFrontResourcesService resourcesService;
    private final StoreFrontLaunchInfoRepository launchInfoRepository;

    public ActivityResponse getActiveSessions(AuthUserDto authUser) {

        log.info("getActiveSessions started");

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", String.format("CtxsDeviceId=%s; CsrfToken=%s; ASP.NET_SessionId=%s; CtxsAuthId=%s",
                launchInfoRepository.findByUsername(authUser.getUsername()).getCtxsDeviceId(),
                authUser.getCitrixCsrfToken(),
                authUser.getCitrixSessionId(),
                authUser.getCitrixAuthId()));
        headers.set("Csrf-Token", authUser.getCitrixCsrfToken());
        headers.set("X-Citrix-IsUsingHTTPS", "No");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                BASE_URL + "/Sessions/List",
                HttpMethod.GET,
                entity,
                ActivityResponse.class
        ).getBody();
    }

    public void shutdownMachine(AuthUserDto authUser, String machineId) {

        log.info("shutdownMachine started");

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", String.format("CtxsDeviceId=%s; CsrfToken=%s; ASP.NET_SessionId=%s; CtxsAuthId=%s",
                launchInfoRepository.findByUsername(authUser.getUsername()).getCtxsDeviceId(),
                authUser.getCitrixCsrfToken(),
                authUser.getCitrixSessionId(),
                authUser.getCitrixAuthId()));
        headers.set("Csrf-Token", authUser.getCitrixCsrfToken());
        headers.set("X-Citrix-IsUsingHTTPS", "No");

        // Request body 생성
        Map<String, String> requestBody = Map.of("machineId", machineId);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        restTemplate.exchange(
                BASE_URL + "/MachineControl/ShutDown",
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    public String checkMachinePowerStateWithPolling(AuthUserDto authUser, String machineId) throws InterruptedException {
        boolean wasInTurningOff = false;
        int maxAttempts = 30;  // 최대 30번 시도
        int attempt = 0;

        while (attempt < maxAttempts) {
            ActivityResponse sessions = getActiveSessions(authUser);
            log.info("Checking power state - Attempt {}: Sessions size {}",
                    attempt + 1, sessions.getSessions().size());

            // 세션이 비어있고 이전에 TurningOff였다면 종료로 간주
            if (sessions.getSessions().isEmpty() && wasInTurningOff) {
                log.info("Machine confirmed shutdown - waiting for system stabilization");
                Thread.sleep(180000);  // 시스템 안정화를 위한 3분 대기
                return "Off";
            }

            // 현재 세션 상태 확인
            Optional<ActivitySession> session = findMachineSession(sessions, machineId);
            if (session.isPresent()) {
                String powerState = session.get().getMachineData().getPowerState();
                log.info("Current power state: {}", powerState);

                if ("TurningOff".equals(powerState)) {
                    wasInTurningOff = true;
                }
            }

            attempt++;
            Thread.sleep(2000);  // 2초 간격으로 체크
        }

        throw new RuntimeException("Failed to confirm machine shutdown after maximum attempts");
    }

    private Optional<ActivitySession> findMachineSession(ActivityResponse sessions, String machineId) {
        return sessions.getSessions().stream()
                .filter(session -> session.getMachineData().getMachineId().equals(machineId))
                .findFirst();
    }

}
