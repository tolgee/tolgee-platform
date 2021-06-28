package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Length
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@Validated
data class CreateKeyDto(
        /**
         * Key full path is stored as name in entity
         */
        @Schema(description = "Name of the key")
        @field:NotBlank
        @field:Length(max = 200, min = 1)
        val name: String = "",
)
