package com.polygloat.dtos;

import com.polygloat.configuration.AppConfiguration;
import lombok.Getter;

public class PublicConfigurationDTO {
    @Getter
    private boolean authentication;

    @Getter
    private AuthMethodsDTO authMethods;

    @Getter
    private boolean passwordResettable;

    @Getter
    private boolean allowRegistrations;

    public PublicConfigurationDTO(AppConfiguration configuration) {
        this.authentication = configuration.isAuthentication();
        if (authentication) {
            authMethods = new AuthMethodsDTO(new GithubPublicConfigDTO(configuration.getGithubClientId()));
        }
        passwordResettable = configuration.isNativeAuth();
        allowRegistrations = configuration.isAllowRegistrations();
    }

    public static class AuthMethodsDTO {
        @Getter
        private GithubPublicConfigDTO github;

        public AuthMethodsDTO(GithubPublicConfigDTO github) {
            this.github = github;
        }
    }

    public static class GithubPublicConfigDTO {
        @Getter
        private boolean enabled;

        @Getter
        private String clientId;

        public GithubPublicConfigDTO(String clientId) {
            this.clientId = clientId;
            this.enabled = clientId != null && !clientId.isEmpty();
        }
    }
}
