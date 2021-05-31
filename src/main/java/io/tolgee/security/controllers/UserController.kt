package io.tolgee.security.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.UserUpdateRequestDTO
import io.tolgee.dtos.response.UserResponseDTO
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.UserAccountService
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "Manipulates user data")
class UserController(
        private val authenticationFacade: AuthenticationFacade,
        private val userAccountService: UserAccountService,
) {
    @Operation(summary = "Returns current user's data")
    @GetMapping("")
    fun getInfo(): UserResponseDTO {
        val userAccount = authenticationFacade.userAccount;
        return UserResponseDTO(
                name = userAccount.name,
                username = userAccount.username,
                id = userAccount.id,
                emailAwaitingVerification = userAccount.emailVerification?.newEmail
        )
    }

    @PostMapping("")
    @Operation(summary = "Updates current user's data")
    fun updateUser(@RequestBody @Valid dto: UserUpdateRequestDTO?) {
        userAccountService.update(authenticationFacade.userAccount, dto!!)
    }
}
