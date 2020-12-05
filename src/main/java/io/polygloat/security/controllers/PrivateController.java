package io.polygloat.security.controllers;

import io.polygloat.security.AuthenticationFacade;
import io.polygloat.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class PrivateController {
    protected AuthenticationFacade authenticationFacade;
    protected SecurityService securityService;

    public PrivateController() {
    }


    @Autowired
    public void setAuthenticationFacade(AuthenticationFacade authenticationFacade) {
        this.authenticationFacade = authenticationFacade;
    }

    @Autowired
    public void setAuthenticationFacade(SecurityService securityService) {
        this.securityService = securityService;
    }

}
