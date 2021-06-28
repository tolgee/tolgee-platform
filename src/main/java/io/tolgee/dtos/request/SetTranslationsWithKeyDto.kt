package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Validated
data class SetTranslationsWithKeyDto(
        /**
         * Key full path is stored as name in entity
         */
        @Schema(description = "Key name to set translations for", example = "what_a_key_to_translate")
        @field:NotNull @field:NotBlank
        val key: String = "",

        /**
         * Map of language tag -> text
         */
        @field:NotNull
        @Schema(description = "Object mapping language tag to translation",
                example = "{\"en\": \"What a translated value!\", \"cs\": \"Jaká to přeložená hodnota!\"}")
        val translations: Map<String, String?> = mapOf()
)
