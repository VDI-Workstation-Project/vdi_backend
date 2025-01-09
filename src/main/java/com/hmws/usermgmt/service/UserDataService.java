package com.hmws.usermgmt.service;

import com.hmws.personnel_info.domain.PersonnelInfo;
import com.hmws.personnel_info.domain.PersonnelNotFoundException;
import com.hmws.personnel_info.dto.UserRegistrationRequestDto;
import com.hmws.personnel_info.repository.PersonnelInfoRepository;
import com.hmws.portal_log_in_records.constant.LogInStatus;
import com.hmws.portal_log_in_records.domain.PortalLogInRecord;
import com.hmws.portal_log_in_records.dto.PortalLoginRecordDto;
import com.hmws.portal_log_in_records.repository.PortalLoginRecordRepository;
import com.hmws.usermgmt.constant.UserRole;
import com.hmws.usermgmt.domain.UserData;
import com.hmws.usermgmt.dto.UserDataDto;
import com.hmws.usermgmt.repository.UserDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDataService {

    private final UserDataRepository userDataRepository;

    private final PortalLoginRecordRepository portalLoginRecordRepository;



    public UserDataDto getUserByUserId(String userId, LocalDateTime logInAttemptedTime) {

        UserData user = userDataRepository.findByUserId(userId).orElseThrow(()
                -> new RuntimeException("User not found"));

        PortalLogInRecord record = PortalLogInRecord.builder()
                .userData(user)
                .logInTime(logInAttemptedTime)
                .ipAddress("127.0.0.1")
                .isLogInSuccessful(false)
                .logInStatus(LogInStatus.ATTEMPTED)
                .build();

        System.out.println("로그인 기록: " + record);

        user.getPortalLogInRecord().add(record);
        portalLoginRecordRepository.save(record);

        return new UserDataDto(
                user.getUserNumber(),
                user.getUserIp(),
                user.getUserId(),
                user.getUserPassword().getPasswordId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getSecurityGroup(),
                user.getOrganizationalUnitPath(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDeletedAt(),
                user.isDeleted(),
                user.isActive(),
                record.getRecordId(),
                user.getRegion());
    }


    private final PersonnelInfoRepository personnelInfoRepository;

    @Value("${ad.default.password}")
    private String defaultPassword;

    @Value("${ad.default.email-domain}")
    private String emailDomain;

    @Value("${ad.userScript.path}")
    private String adScriptPath;

    @Value("${ad.server.ip}")
    private String adServerIp;

    @Value("${ad.admin.username}")
    private String adAdminUsername;

    @Value("${ad.admin.password}")
    private String adAdminPassword;

    public void createAdUser(UserRegistrationRequestDto request) {

        // 입력 검증
        if (request.getUserId() == null || request.getFirstName() == null ||
                request.getLastName() == null || request.getPhoneNumber() == null) {
            throw new IllegalArgumentException("모든 필드는 필수입니다.");
        }

        PersonnelInfo personnelInfo = personnelInfoRepository.findByFirstNameAndLastNameAndPhoneNumber(
                request.getFirstName(), request.getLastName(), request.getPhoneNumber()
        ).orElseThrow(() -> new RuntimeException("인사정보를 찾을 수 없습니다."));

//        boolean exists = personnelInfoRepository.existsByFirstNameAndLastNameAndPhoneNumber(
//                request.getFirstName(),
//                request.getLastName(),
//                request.getPhoneNumber()
//        );
//
//        if (!exists) {
//            throw new PersonnelNotFoundException("인사정보를 찾을 수 없습니다.");
//        }
//
//        UserRole role = personnelInfoRepository.findRoleByFirstNameAndLastNameAndPhoneNumber(
//                request.getFirstName(), request.getLastName(), request.getPhoneNumber()
//        );

        UserRole role = personnelInfo.getRole();

        String email = String.format("%s@%s", request.getUserId(), emailDomain);


        try {
            // 스크립트 파일 체크
            File scriptFile = new File(adScriptPath);
            log.info("스크립트 절대 경로: {}", scriptFile.getAbsolutePath());
            log.info("스크립트 파일 존재 여부: {}", scriptFile.exists());

            // PowerShell 테스트
            log.info("PowerShell 테스트 시작");
            ProcessBuilder testBuilder = new ProcessBuilder(
                    "powershell.exe",
                    "-Command",
                    "Write-Host 'PowerShell Test Success'; $PSVersionTable.PSVersion"
            );

            testBuilder.redirectErrorStream(true);
            Process testProcess = testBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(testProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("PowerShell 테스트 출력: {}", line);
                }
            }

            int testExitCode = testProcess.waitFor();
            log.info("PowerShell 테스트 종료 코드: {}", testExitCode);

            String ouPath = String.format("OU=%s,DC=hm,DC=com", personnelInfo.getDepartment());

            // 실제 AD 사용자 생성 명령 준비
            List<String> commands = Arrays.asList(
                    "powershell.exe",
                    "-ExecutionPolicy", "Bypass",
                    "-NoProfile",
                    "-NonInteractive",
                    "-File",
                    adScriptPath,
                    "-UserId", request.getUserId(),
                    "-FirstName", request.getFirstName(),
                    "-LastName", request.getLastName(),
                    "-Password", defaultPassword,
                    "-PhoneNumber", request.getPhoneNumber(),
                    "-Email", email,
                    "-Title", role.getRole(),
                    "-ADServer", adServerIp,
                    "-Username", adAdminUsername,
                    "-AdminPassword", adAdminPassword,
                    "-OUPath", ouPath,
                    "-SecurityGroupName", String.format("SG_%s_%s", personnelInfo.getDepartment(), request.getUserId())
            );

            log.info("실행할 PowerShell 명령어: {}", String.join(" ", commands));

            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.redirectErrorStream(true);

            // 프로세스 시작 전 잠시 대기
            Thread.sleep(1000);

            log.info("AD 사용자 생성 프로세스 시작...");
            Process process = processBuilder.start();

            // 출력 스트림 읽기를 위한 별도 스레드
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("PowerShell 출력: {}", line);
                    }
                } catch (IOException e) {
                    log.error("출력 읽기 실패", e);
                }
            });

            outputThread.start();

            // 프로세스 완료 대기 (타임아웃 설정)
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("PowerShell 스크립트 실행 시간 초과");
            }

            // 출력 스레드 완료 대기
            outputThread.join();

            int exitCode = process.exitValue();
            log.info("PowerShell 종료 코드: {}", exitCode);

            if (exitCode != 0) {
                String errorMessage = switch (exitCode) {
                    case 1 -> "PowerShell 스크립트 실행 중 오류 발생";
                    case -196608 -> "PowerShell 실행 권한 문제 발생";
                    default -> "알 수 없는 오류 (Exit Code: " + exitCode + ")";
                };
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }

            log.info("AD 사용자 생성 완료: {}", request.getUserId());

        } catch (IOException e) {
            log.error("PowerShell 프로세스 시작 실패", e);
            throw new RuntimeException("PowerShell 실행 실패", e);
        } catch (InterruptedException e) {
            log.error("PowerShell 프로세스 대기 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("PowerShell 실행 중단", e);
        } catch (Exception e) {
            log.error("AD 사용자 생성 중 예기치 않은 오류 발생", e);
            throw new RuntimeException("AD 사용자 생성 실패", e);
        }
    }
}
