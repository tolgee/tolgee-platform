package com.polygloat.security.controllers;

import com.polygloat.security.AuthenticationFacade;
import com.polygloat.service.SecurityService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@NoArgsConstructor
public abstract class PrivateController {
    protected AuthenticationFacade authenticationFacade;
    protected SecurityService securityService;


    @Autowired
    public void setAuthenticationFacade(AuthenticationFacade authenticationFacade) {
        this.authenticationFacade = authenticationFacade;
    }

    @Autowired
    public void setAuthenticationFacade(SecurityService securityService) {
        this.securityService = securityService;
    }

}
