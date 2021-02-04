package io.tolgee.security;

import io.tolgee.configuration.tolgee.TolgeeProperties;
import io.tolgee.model.ApiKey;
import io.tolgee.model.UserAccount;
import io.tolgee.security.api_key_auth.ApiKeyAuthenticationToken;
import io.tolgee.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFacade {
    private TolgeeProperties configuration;
    private UserAccountService userAccountService;

    AuthenticationFacade(TolgeeProperties configuration, UserAccountService userAccountService) {
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