package io.tolgee.security.controllers;

import io.tolgee.dtos.request.UserUpdateRequestDTO;
import io.tolgee.dtos.response.UserResponseDTO;
import io.tolgee.security.AuthenticationFacade;
import io.tolgee.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthenticationFacade authenticationFacade;
    private final UserAccountService userAccountService;

    @Autowired
    public UserController(AuthenticationFacade authenticationFacade, UserAccountService userAccountService) {
        this.authenticationFacade = authenticationFacade;
        this.userAccountService = userAccountService;
    }

    @GetMapping("")
    public UserResponseDTO getInfo() {
        return UserResponseDTO.fromEntity(authenticationFacade.getUserAccount());
    }

    @PostMapping("")
    public void updateUser(@RequestBody @Valid UserUpdateRequestDTO dto) {
        userAccountService.update(authenticationFacade.getUserAccount(), dto);
    }
}

