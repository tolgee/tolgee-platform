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
        var key: String? = null,

        /**
         * Map of language tag -> text
         */
        @field:NotNull
        var translations: Map<String, String?>? = null
)
