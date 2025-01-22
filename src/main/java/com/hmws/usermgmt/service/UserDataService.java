package com.hmws.usermgmt.service;

import com.hmws.personnel_info.domain.PersonnelInfo;
import com.hmws.personnel_info.dto.UserRegistrationRequestDto;
import com.hmws.personnel_info.repository.PersonnelInfoRepository;
import com.hmws.portal_log_in_records.constant.LogInStatus;
import com.hmws.portal_log_in_records.domain.PortalLogInRecord;
import com.hmws.portal_log_in_records.repository.PortalLoginRecordRepository;
import com.hmws.usermgmt.domain.UserData;
import com.hmws.usermgmt.dto.UserDataDto;
import com.hmws.usermgmt.repository.UserDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.time.LocalDateTime;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

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
                user.getUserId(),
                user.getUserPasswordLogs().getPasswordId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getTelephone(),
                user.getMobile(),
                user.getUserType(),
                user.getUserRole(),
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
    private final LdapTemplate ldapTemplate;

    @Value("${spring.ldap.base}")
    private String ldapBase;

    @Value("${spring.ldap.urls}")
    private String ldapUrl;

    @Value("${spring.ldap.username}")
    private String ldapUsername;

    @Value("${spring.ldap.password}")
    private String ldapPassword;

    @Value("${ad.default.password}")
    private String defaultPassword;

    @Value("${ad.default.email-domain}")
    private String emailDomain;

    @Transactional
    public void createAdUser(UserRegistrationRequestDto request) {

        // 입력 검증
        if (request.getUserId() == null || request.getFirstName() == null ||
                request.getLastName() == null || request.getPhoneNumber() == null) {
            throw new IllegalArgumentException("모든 필드는 필수입니다.");
        }

        PersonnelInfo personnelInfo = personnelInfoRepository.findByFirstNameAndLastNameAndMobile(
                request.getFirstName(), request.getLastName(), request.getPhoneNumber()
        ).orElseThrow(() -> new RuntimeException("인사정보를 찾을 수 없습니다."));

        String email = String.format("%s@%s", request.getUserId(), emailDomain);

        String securityGroupName = String.format("SG_%s_%s", personnelInfo.getDepartment(), request.getUserId());

        try {

            log.info("create AD User try-catch entrance");

            String ouPath = String.format("OU=%s", personnelInfo.getDepartment());

            // 사용자 DN 구성 - Base DN은 자동으로 추가됨
            String userDN = String.format("CN=%s %s,%s",
                    request.getFirstName(),
                    request.getLastName(),
                    ouPath);

            log.info("Attempting to create user with DN: {}", userDN);

            DirContextOperations context = new DirContextAdapter(userDN);

            context.setAttributeValues("objectclass",
                    new String[] {"top", "person", "organizationalPerson", "user"});

            // 디버그 로그 추가
            log.debug("LDAP 연결 URL: {}", ldapUrl);
            log.debug("LDAP Base DN: {}", ldapBase);
            log.debug("사용자 DN: {}", context.getDn());

            // 필수 속성 설정
            context.setAttributeValue("cn", String.format("%s %s", request.getFirstName(), request.getLastName()));
            context.setAttributeValue("sAMAccountName", request.getUserId());
            context.setAttributeValue("userPrincipalName", email);
            context.setAttributeValue("givenName", request.getFirstName());
            context.setAttributeValue("sn", request.getLastName());
            context.setAttributeValue("displayName", String.format("%s %s", request.getFirstName(), request.getLastName()));

            // 추가 속성 설정
            context.setAttributeValue("mail", email);
            context.setAttributeValue("telephoneNumber", personnelInfo.getTelephone());
            context.setAttributeValue("mobile", request.getPhoneNumber());
            context.setAttributeValue("title", personnelInfo.getRole().name());
            context.setAttributeValue("department", personnelInfo.getDepartment());

            // 계정 옵션 설정 (512: 일반 계정, 544: 일반 계정 + 비밀번호 변경 필요)
            int userAccountControl = 544;  // 첫 로그인 시 비밀번호 변경 필요
            context.setAttributeValue("userAccountControl", String.valueOf(userAccountControl));

            // 비밀번호 설정
            String quotedPassword = "\"" + defaultPassword + "\"";
            byte[] passwordBytes = quotedPassword.getBytes("UTF-16LE");
            context.setAttributeValue("unicodePwd", passwordBytes);

            // 계정 활성화를 위한 추가 속성
            context.setAttributeValue("pwdLastSet", "0");  // 다음 로그인 시 비밀번호 변경 필요
            context.setAttributeValue("accountExpires", "0");  // 계정 만료 없음

            log.info("context attribute values: {}", context.getAttributes().toString());

            // LDAP에 사용자 생성 전에 로그 추가
            log.debug("Creating user with attributes: {}", context.getAttributes());

            // LDAP에 사용자 생성
            ldapTemplate.bind(context);

            // 보안 그룹 생성 및 할당
            createAndAssignSecurityGroup(securityGroupName, userDN, personnelInfo.getDepartment());

            log.info("AD 사용자 생성 완료: {}", request.getUserId());

        } catch (Exception e) {
            log.error("AD 사용자 생성 중 오류 발생", e);
            throw new RuntimeException("AD 사용자 생성 실패: " + e.getMessage());
        }

    }

    private void createAndAssignSecurityGroup(String securityGroupName, String userDN, String department) {

        try {
            String groupDN = String.format("CN=%s,OU=%s", securityGroupName, department);

            // 보안 그룹이 존재하지 않으면 생성
            if (!securityGroupExists(securityGroupName)) {
                DirContextOperations groupContext = new DirContextAdapter(groupDN);

                groupContext.setAttributeValues("objectclass",
                        new String[] {"top", "group"});
                groupContext.setAttributeValue("sAMAccountName", securityGroupName);
                // 도메인 로컬 보안 그룹으로 설정
                // -2147483644 = ADS_GROUP_TYPE_DOMAIN_LOCAL_GROUP | ADS_GROUP_TYPE_SECURITY_ENABLED
                groupContext.setAttributeValue("groupType", "-2147483644");

                ldapTemplate.bind(groupContext);
            }

            // PowerShell의 Add-ADGroupMember는 sAMAccountName을 사용하지만
            // LDAP에서는 전체 DN이 필요합니다
            String fullUserDN = String.format("%s,%s", userDN, ldapBase);

            // 사용자를 그룹에 추가
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE,
                    new BasicAttribute("member", fullUserDN));

            ldapTemplate.modifyAttributes(groupDN, mods);

        } catch (Exception e) {
            log.error("보안 그룹 생성/할당 중 오류 발생", e);
            throw new RuntimeException("보안 그룹 작업 실패: " + e.getMessage());
        }
    }

    private boolean securityGroupExists(String securityGroupName) {

        try {
            return ldapTemplate.search(
                    query()
                            .base(ldapBase)  // 검색 시작 위치 지정
                            .where("objectClass").is("group")
                            .and("sAMAccountName").is(securityGroupName),
                    new AttributesMapper<Boolean>() {
                        @Override
                        public Boolean mapFromAttributes(Attributes attrs) throws NamingException {
                            return attrs.get("sAMAccountName") != null;
                        }
                    }
            ).stream().findFirst().orElse(false);

        } catch (Exception e) {
            log.error("보안 그룹 검색 중 오류: {}", e.getMessage());
            return false;
        }

    }
}
