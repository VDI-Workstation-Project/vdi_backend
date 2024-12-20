package com.hmws.vm_mgmt.sub_vms.service;

import com.hmws.personnel_info.domain.PersonnelInfo;
import com.hmws.usermgmt.domain.UserData;
import com.hmws.usermgmt.repository.UserDataRepository;
import com.hmws.vm_mgmt.sub_vms.constants.VmStatus;
import com.hmws.vm_mgmt.sub_vms.domain.SubVms;
import com.hmws.vm_mgmt.sub_vms.repository.SubVmsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubVmsService {

    private final SubVmsRepository subVmsRepository;

    private final UserDataRepository userDataRepository;

    @Value("${ad.baseVmxTemplate.path}")
    private String baseVmxTemplatePath;

    @Value("${ad.vmCreatedIn.path}")
    private String vmCreatedIn;

    @Value("${ad.server.ip}")
    private String adServerIp;

    @Value("${ad.securityScript.path}")
    private String adSecurityScriptPath;

    @Value("${ad.admin.username}")
    private String adAdminUsername;

    @Value("${ad.admin.password}")
    private String adAdminPassword;

    @Transactional
    public SubVms createVm(String userId) throws IOException {

        UserData user = userDataRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));


        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"));

        String vmName = String.format("vm-%s", timeStamp);

        // 새 VM을 위한 디렉토리 생성
        Path vmDirectory = Paths.get(vmCreatedIn, vmName);
        Files.createDirectories(vmDirectory);

        // Template을 바탕으로한 새 vmx파일 경로 설정
        String vmxPath = vmDirectory.resolve(vmName + ".vmx").toString();

        // VMX 파일 복사 및 수정
        modifyVmxFile(baseVmxTemplatePath, vmxPath, vmName);

        // VMDK파일 복사
        Path sourceVmdkPath = Paths.get(baseVmxTemplatePath).getParent();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceVmdkPath, "*.vmdk")) {
            for (Path sourcePath : stream) {
                String sourceFileName = sourcePath.getFileName().toString();
                String targetFileName = sourceFileName.replace("MASTER_VM", vmName);
                Path targetPath = vmDirectory.resolve(targetFileName);
                Files.copy(sourcePath, targetPath);
            }
        }

        SubVms vm = SubVms.builder()
                .userData(user)
                .subVmName(vmName)
                .subVmStatus(VmStatus.POWERED_OFF)
                .build();

        return subVmsRepository.save(vm);

    }

    private void modifyVmxFile(String sourcePath, String targetPath, String vmName) throws IOException {

        try (BufferedReader reader = new BufferedReader(new FileReader(sourcePath));
                BufferedWriter writer = new BufferedWriter(new FileWriter(targetPath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 이름 관련 설정만 수정
                if (line.startsWith("displayName")) {
                    line = "displayName = \"" + vmName + "\"";
                } else if (line.startsWith("nvram")) {
                    line = "nvram = \"" + vmName + ".nvram\"";
                } else if (line.startsWith("extendedConfigFile")) {
                    line = "extendedConfigFile = \"" + vmName + ".vmxf\"";
                } else if (line.startsWith("nvme0:0.fileName")) {
                    line = "nvme0:0.fileName = \"" + vmName + ".vmdk\"";
                } else if (line.startsWith("vmxstats.filename")) {
                    line = "vmxstats.filename = \"" + vmName + ".scoreboard\"";
                }

                writer.write(line);
                writer.newLine();
            }

        }

    }

    public void setVmSecurity(String userId) throws IOException, InterruptedException {
        UserData user = userDataRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubVms subVm = subVmsRepository.findByUserData_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Created VM not found"));

        log.info("Starting VM security configuration - User: {}, OU: {}, SG: {}, AD Server: {}",
                userId,
                user.getOrganizationalUnitPath(),
                user.getSecurityGroup(),
                adServerIp);

        String vmxPath = Paths.get(vmCreatedIn, subVm.getSubVmName(),
                subVm.getSubVmName() + ".vmx").toString();

        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-ExecutionPolicy", "Bypass",
                "-File", adSecurityScriptPath,
                "-VmxPath", vmxPath,
                "-UserId", userId,
                "-AdminUsername", adAdminUsername,
                "-AdminPassword", adAdminPassword,
                "-OUPath", user.getOrganizationalUnitPath(),
                "-SecurityGroupName", user.getSecurityGroup(),
                "-ADServer", adServerIp,  // IP 주소 전달
                "-VmName", subVm.getSubVmName()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        boolean securityConfigCompleted = false;
        boolean hasErrors = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info("PowerShell output: {}", line);

                // 성공 여부 확인
                if (line.contains("VM security configuration completed")) {
                    securityConfigCompleted = true;
                }

                // 오류 검사
                if (line.contains("Error") ||
                        line.contains("failed") ||
                        line.contains("exception") ||
                        line.contains("ParserError") ||
                        line.contains("Missing terminator") ||
                        line.contains("Missing closing '}'") ||
                        line.contains("TerminatorExpectedAtEndOfString") ||
                        line.contains("The running command stopped") ||
                        line.contains("Cannot connect to") ||
                        line.contains("Access is denied")) {
                    hasErrors = true;
                    log.error("Error detected in PowerShell output: {}", line);
                }
            }
        }

        int exitCode = process.waitFor();
        String outputStr = output.toString();

        // 실패 조건 검사
        if (exitCode != 0 || hasErrors || !securityConfigCompleted) {
            String errorMessage = String.format(
                    "VM security configuration failed:%n" +
                            "Exit Code: %d%n" +
                            "Has Errors: %b%n" +
                            "Configuration Completed: %b%n" +
                            "Full Output:%n%s",
                    exitCode, hasErrors, securityConfigCompleted, outputStr
            );

            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        log.info("VM security configuration successfully completed - User: {}", userId);
    }
}
