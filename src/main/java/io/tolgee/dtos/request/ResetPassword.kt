package io.tolgee.dtos.request

import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

data class ResetPassword(
        @field:NotBlank
        var email: String? = null,

        @field:NotBlank
        var code: String? = null,

        @field:Min(8) @Max(100)
        var password: String? = null,
)
