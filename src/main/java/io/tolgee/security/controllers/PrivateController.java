package io.tolgee.security.controllers;

import io.tolgee.security.AuthenticationFacade;
import io.tolgee.service.SecurityService;
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
