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

    private final String baseVmxTemplatePath = "C:\\vm_base_template\\template.vmx";

    private final String vmCreatedIn = "C:\\win2022";

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

    public void setVmSecurity(String userId) throws IOException, InterruptedException{

        UserData user = userDataRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        // VM 이름 가져오기 (가장 최근에 생성된 VM)
        SubVms subVm = subVmsRepository.findByUserData_UserId(userId)
                .orElseThrow(() -> new RuntimeException("생성된 VM을 찾을 수 없습니다"));

        log.info("VM 보안 설정 시작 - 사용자: {}, OU: {}, SG: {}, AD서버: {}",
                userId,
                user.getOrganizationalUnitPath(),
                user.getSecurityGroup(),
                adServerIp);


        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-ExecutionPolicy", "Bypass",
                "-File", adSecurityScriptPath,
                "-VmPath", vmCreatedIn,
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

        // 실시간으로 PowerShell 출력 로깅
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info("PowerShell 출력: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("VM 보안 설정 실패 - 종료 코드: {}, 전체 출력:\n{}", exitCode, output.toString());
            throw new RuntimeException("VM 보안 설정 실패 - PowerShell 스크립트 오류");
        }

        log.info("VM 보안 설정 완료 - 사용자: {}", userId);
    }
}
