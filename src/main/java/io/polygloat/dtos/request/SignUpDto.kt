package io.polygloat.dtos.request

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class SignUpDto(
        @NotBlank
        var name: String? = null,

        @Email @NotBlank
        var email: String? = null,

        @Size(min = 8, max = 100)
        @NotBlank
        var password: String? = null,

        var invitationCode: String? = null,

        var callbackUrl: String? = null
)