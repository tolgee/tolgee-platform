package io.polygloat.dtos.request

import org.springframework.validation.annotation.Validated
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

@Validated
data class ResetPasswordRequest(
        @NotBlank
        var callbackUrl: String? = null,

        @Email
        var email: String? = null
)