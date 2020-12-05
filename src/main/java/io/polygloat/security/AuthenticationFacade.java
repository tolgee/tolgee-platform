package io.polygloat.security;

import io.polygloat.configuration.polygloat.PolygloatProperties;
import io.polygloat.model.ApiKey;
import io.polygloat.model.UserAccount;
import io.polygloat.security.api_key_auth.ApiKeyAuthenticationToken;
import io.polygloat.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFacade {
    private PolygloatProperties configuration;
    private UserAccountService userAccountService;

    AuthenticationFacade(PolygloatProperties configuration, UserAccountService userAccountService) {
        this.configuration = configuration;
        this.userAccountService = userAccountService;
    }

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public UserAccount getUserAccount() {
        if (!configuration.getAuthentication().getEnabled()) {
            return userAccountService.getImplicitUser();
        }

        return (UserAccount) this.getAuthentication().getPrincipal();
    }

    public ApiKey getApiKey() {
        ApiKeyAuthenticationToken authentication = (ApiKeyAuthenticationToken) this.getAuthentication();

        return authentication.getApiKey();
    }
}