package io.polygloat.security.controllers;

import io.polygloat.security.AuthenticationFacade;
import io.polygloat.service.SecurityService;
import lombok.NoArgsConstructor;
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
