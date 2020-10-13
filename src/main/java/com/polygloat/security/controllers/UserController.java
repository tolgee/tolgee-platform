package com.polygloat.security.controllers;

import com.polygloat.dtos.request.UserUpdateRequestDTO;
import com.polygloat.dtos.response.UserResponseDTO;
import com.polygloat.security.AuthenticationFacade;
import com.polygloat.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController {

    private final AuthenticationFacade authenticationFacade;
    private final UserAccountService userAccountService;

    @GetMapping("")
    public UserResponseDTO getInfo() {
        return UserResponseDTO.fromEntity(authenticationFacade.getUserAccount());
    }

    @PostMapping("")
    public void updateUser(@RequestBody @Valid UserUpdateRequestDTO dto) {
        userAccountService.update(authenticationFacade.getUserAccount(), dto);
    }
}

