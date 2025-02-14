package com.hmws.global.ldap.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class LdapConfig {

    @Value("${spring.ldap.urls}")
    private String ldapUrl;

    @Value("${spring.ldap.base}")
    private String ldapBase;

    @Value("${spring.ldap.username}")
    private String ldapUsername;

    @Value("${spring.ldap.password}")
    private String ldapPassword;

    @Value("${spring.ldap.allowed-networks}")
    private String allowedNetworks;

    @PostConstruct
    public void init() {
        // SSL 디버깅 활성화
        System.setProperty("javax.net.debug", "ssl,handshake");
        // JNDI 디버깅 활성화
        System.setProperty("com.sun.jndi.ldap.trace.ber", "true");
    }

    @Bean
    public LdapContextSource contextSource() {


        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(ldapBase);
        contextSource.setUserDn(ldapUsername);
        contextSource.setPassword(ldapPassword);

        Map<String, Object> baseEnv = new HashMap<>();
        baseEnv.put(Context.SECURITY_PROTOCOL, "ssl");
        baseEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        // Active Directory 특정 설정
        baseEnv.put("java.naming.ldap.attributes.binary", "objectSID objectGUID");
        baseEnv.put(Context.REFERRAL, "follow");

        // Java Home 경로를 이용한 키스토어 설정
        String javaHome = System.getProperty("java.home");
        // 수정된 코드 (Paths.get 사용)
        String trustStorePath = Paths.get(javaHome, "lib", "security", "cacerts").toString();

        log.info("Using trustStore path: {}", trustStorePath);

        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "qw12qw!@");

        contextSource.setBaseEnvironmentProperties(baseEnv);

        // 연결 테스트
        try {
            log.info("Testing LDAP connection...");
            contextSource.afterPropertiesSet();
            DirContext ctx = contextSource.getContext(ldapUsername, ldapPassword);
            log.info("LDAP connection successful!");
            ctx.close();
        } catch (Exception e) {
            log.error("LDAP connection failed: ", e);
        }

        return contextSource;

    }

    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource contextSource) {

        LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.setIgnoreNameNotFoundException(true);

        return ldapTemplate;
    }
}
