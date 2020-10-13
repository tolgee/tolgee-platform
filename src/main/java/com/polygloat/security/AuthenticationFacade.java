package com.polygloat.security;

import com.polygloat.configuration.AppConfiguration;
import com.polygloat.constants.Message;
import com.polygloat.exceptions.AuthenticationException;
import com.polygloat.model.ApiKey;
import com.polygloat.model.UserAccount;
import com.polygloat.security.api_key_auth.ApiKeyAuthenticationToken;
import com.polygloat.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFacade {
    private AppConfiguration configuration;
    private UserAccountService userAccountService;

    AuthenticationFacade(AppConfiguration configuration, UserAccountService userAccountService) {

        this.configuration = configuration;
        this.userAccountService = userAccountService;
    }

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public UserAccount getUserAccount() {
        if (!configuration.isAuthentication()) {
            return userAccountService.getImplicitUser();
        }

        return (UserAccount) this.getAuthentication().getPrincipal();
    }

    public ApiKey getApiKey() {
        ApiKeyAuthenticationToken authentication = (ApiKeyAuthenticationToken) this.getAuthentication();

        return authentication.getApiKey();
    }
}
