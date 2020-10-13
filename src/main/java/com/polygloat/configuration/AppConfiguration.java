package com.polygloat.configuration;


import com.polygloat.constants.Message;
import com.polygloat.exceptions.BadRequestException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class AppConfiguration {


    /**
     * Auth settings
     */

    @Value("${app.jwtExpirationInMs:604800000}")
    private Long jwtExpirationInMs;

    @Value("${polygloat.authentication:true}")
    private boolean authentication;

    @Value("${polygloat.security.github.client-secret:#{null}}")
    private String githubClientSecret;

    @Value("${polygloat.security.github.client-id:#{null}}")
    private String githubClientId;

    @Value("${polygloat.security.github.authorization-link:#{null}}")
    private String githubAuthorizationLink;

    @Value("${polygloat.security.github.user-link:#{null}}")
    private String githubUserLink;

    @Value("${polygloat.auth.native:false}")
    private boolean nativeAuth;

    @Value("${polygloat.auth.allow-registrations:false}")
    private boolean allowRegistrations;

    @Value("${polygloat.ldap.enabled:false}")
    private boolean ldapAuthentication;

    @Value("${spring.ldap.embedded.port:#{null}}")
    private String ldapPort;
    //Getting values from properties file
    @Value("${polygloat.ldap.url:#{null}}")
    private String ldapUrls;
    @Value("${polygloat.ldap.base.dn:#{null}}")
    private String ldapBaseDn;
    @Value("${polygloat.ldap.username:#{null}}")
    private String ldapSecurityPrincipal;
    @Value("${polygloat.ldap.password:#{null}}")
    private String ldapPrincipalPassword;
    @Value("${polygloat.ldap.user.dn-pattern:#{null}}")
    private String ldapUserDnPattern;


    /**
     * Mail configuration
     */

    @Value("${polygloat.mail.host:#{null}}")
    private String mailHost;

    @Value("${polygloat.spring.mail.username:#{null}}")
    private String mailUsername;

    @Value("${polygloat.mail.password:#{null}}")
    private String mailPassword;

    @Value("${polygloat.mail.properties.mail.smtp.port:25}")
    private int mailSmtpPort;

    @Value("${polygloat.mail.properties.mail.smtp.auth:false}")
    private Boolean mailSmtpAuth;

    @Value("${polygloat.mail.properties.mail.smtp.starttls.enable:false}")
    private Boolean mailTtlsEnabled;

    @Value("${polygloat.mail.properties.mail.smtp.ssl.enable:false}")
    private Boolean mailSSlEnabled;

    @Value("${polygloat.mail.properties.mail.smtp.starttls.required:false}")
    private Boolean mailTtlsRequired;

    @Value("${polygloat.mail.from:no-reply@example.com}")
    private String mailFrom;

    public void checkAllowedRegistrations() {
        if (!this.isAllowRegistrations()) {
            throw new BadRequestException(Message.REGISTRATIONS_NOT_ALLOWED);
        }
    }
}
