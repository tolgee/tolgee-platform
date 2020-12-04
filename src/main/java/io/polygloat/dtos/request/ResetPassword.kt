package io.polygloat.dtos.request

import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

data class ResetPassword(
        @NotBlank
        var email: String? = null,

        @NotBlank
        var code: String? = null,

        @Min(8) @Max(100)
        var password: String? = null,
)