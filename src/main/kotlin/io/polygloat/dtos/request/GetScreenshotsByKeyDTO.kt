package io.polygloat.dtos.request

import javax.validation.constraints.NotBlank

data class GetScreenshotsByKeyDTO(
        @field:NotBlank
        var key: String? = null
)