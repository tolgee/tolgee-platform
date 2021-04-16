package io.tolgee.security.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.tolgee.dtos.request.UserUpdateRequestDTO;
import io.tolgee.dtos.response.UserResponseDTO;
import io.tolgee.security.AuthenticationFacade;
import io.tolgee.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "Manipulates user data")
public class UserController {

    private final AuthenticationFacade authenticationFacade;
    private final UserAccountService userAccountService;

    @Autowired
    public UserController(AuthenticationFacade authenticationFacade, UserAccountService userAccountService) {
        this.authenticationFacade = authenticationFacade;
        this.userAccountService = userAccountService;
    }

    @GetMapping("")
    @Operation(summary = "Returns current user's data")
    public UserResponseDTO getInfo() {
        return UserResponseDTO.fromEntity(authenticationFacade.getUserAccount());
    }

    @PostMapping("")
    @Operation(summary = "Updates current user's data")
    public void updateUser(@RequestBody @Valid UserUpdateRequestDTO dto) {
        userAccountService.update(authenticationFacade.getUserAccount(), dto);
    }
}

