package io.tolgee.controllers;

import io.tolgee.model.UserAccount;

public class DefaultAuthenticationResult {
    private String token;
    private UserAccount entity;

    public DefaultAuthenticationResult(String token, UserAccount entity) {
        this.token = token;
        this.entity = entity;
    }

    public String getToken() {
        return token;
    }

    public UserAccount getEntity() {
        return entity;
    }
}
