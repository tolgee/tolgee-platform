package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.Parameter

data class GetTranslationsParams(
        @field:Parameter(description = "Languages to be contained in response", example = "en,de")
        val languages: Set<String>? = null,
        @field:Parameter(description = "String to search in key name or translation text")
        val search: String? = null,
        @field:Parameter(description = "Selects only one key with provided name")
        val filterKeyName: String? = null,
        @field:Parameter(description = "Selects only one key with provided id")
        val filterKeyId: Long? = null,
        @field:Parameter(description = "Selects only keys, where translation is missing in any language")
        val filterUntranslatedAny: Boolean = false,
        @field:Parameter(description = "Selects only keys, where translation is provided in any language")
        val filterTranslatedAny: Boolean = false,
        @field:Parameter(
                description = "Selects only keys, where translation is missing in specified language",
                example = "en-US"
        )
        val filterUntranslatedInLang: String? = null,
        @field:Parameter(description = "Selects only keys, where translation is provided in specified language", example = "en-US")
        val filterTranslatedInLang: String? = null,
        @field:Parameter(description = "Selects only keys with screenshots", example = "en-US")
        val filterHasScreenshot: Boolean = false,
        @field:Parameter(description = "Selects only keys without screenshots", example = "en-US")
        val filterHasNoScreenshot: Boolean = false
)
