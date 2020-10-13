package com.polygloat.controllers;

import com.polygloat.model.UserAccount;

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
