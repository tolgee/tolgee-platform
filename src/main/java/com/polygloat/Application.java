package com.polygloat;

import com.polygloat.development.DbPopulatorReal;
import com.polygloat.dtos.request.SignUpDto;
import com.polygloat.service.UserAccountService;
import io.sentry.Sentry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Application {
    @Autowired
    public Application(DbPopulatorReal populator,
                       UserAccountService userAccountService,
                       @Value("${app.populate:false}") boolean populate,
                       @Value("${app.initialUsername:#{null}}") String initialUsername,
                       @Value("${app.initialPassword:#{null}}") String initialPassword,
                       @Value("${sentry.enabled:false}") boolean sentry,
                       @Value("${sentry.dsn:null}") String sentryDSN) {
        if (populate) {
            populator.autoPopulate();
        }

        if (sentry) {
            Sentry.init(sentryDSN);
        }

        if (initialUsername != null && initialPassword != null && !userAccountService.isAnyUserAccount() &&
                userAccountService.getByUserName(initialUsername).isEmpty()) {
            userAccountService.createUser(SignUpDto.builder().email(initialUsername).password(initialPassword).name(initialUsername).build());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
