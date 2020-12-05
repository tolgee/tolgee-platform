package io.polygloat.dtos;

import io.polygloat.configuration.polygloat.PolygloatProperties;

public class PublicConfigurationDTO {
    private boolean authentication;

    private AuthMethodsDTO authMethods;

    private boolean passwordResettable;

    private boolean allowRegistrations;

    public PublicConfigurationDTO(PolygloatProperties configuration) {
        this.authentication = configuration.getAuthentication().getEnabled();
        if (authentication) {
            authMethods = new AuthMethodsDTO(new GithubPublicConfigDTO(configuration.getAuthentication().getGithub().getClientId()));
        }
        passwordResettable = configuration.getAuthentication().getNativeEnabled();
        allowRegistrations = configuration.getAuthentication().getRegistrationsAllowed();
    }

    public boolean isAuthentication() {
        return this.authentication;
    }

    public AuthMethodsDTO getAuthMethods() {
        return this.authMethods;
    }

    public boolean isPasswordResettable() {
        return this.passwordResettable;
    }

    public boolean isAllowRegistrations() {
        return this.allowRegistrations;
    }

    public static class AuthMethodsDTO {
        private GithubPublicConfigDTO github;

        public AuthMethodsDTO(GithubPublicConfigDTO github) {
            this.github = github;
        }

        public GithubPublicConfigDTO getGithub() {
            return this.github;
        }
    }

    public static class GithubPublicConfigDTO {
        private boolean enabled;

        private String clientId;

        public GithubPublicConfigDTO(String clientId) {
            this.clientId = clientId;
            this.enabled = clientId != null && !clientId.isEmpty();
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public String getClientId() {
            return this.clientId;
        }
    }
}
