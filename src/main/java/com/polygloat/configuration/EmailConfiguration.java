package com.polygloat.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfiguration {

    private AppConfiguration appConfiguration;

    @Autowired
    public EmailConfiguration(AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
    }

    @Bean
    public JavaMailSender getMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(appConfiguration.getMailHost());
        mailSender.setPort(appConfiguration.getMailSmtpPort());

        mailSender.setUsername(appConfiguration.getMailUsername());
        mailSender.setPassword(appConfiguration.getMailPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", appConfiguration.getMailSmtpAuth().toString());
        props.put("mail.smtp.ssl.enable", appConfiguration.getMailSSlEnabled().toString());
        props.put("mail.smtp.starttls.enable", appConfiguration.getMailTtlsEnabled().toString());
        props.put("mail.smtp.starttls.required", appConfiguration.getMailTtlsRequired().toString());
        props.put("mail.debug", "true");

        return mailSender;
    }
}
