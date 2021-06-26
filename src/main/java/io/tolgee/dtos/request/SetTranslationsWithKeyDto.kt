package io.tolgee.dtos.request

import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Validated
data class SetTranslationsWithKeyDto(
        /**
         * Key full path is stored as name in entity
         */
        @field:NotNull @field:NotBlank
        val key: String = "",

        /**
         * Map of language tag -> text
         */
        @field:NotNull
        val translations: Map<String, String?> = mapOf()
)
