package io.polygloat.security.controllers;

import io.polygloat.dtos.request.UserUpdateRequestDTO;
import io.polygloat.dtos.response.UserResponseDTO;
import io.polygloat.security.AuthenticationFacade;
import io.polygloat.service.UserAccountService;
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

