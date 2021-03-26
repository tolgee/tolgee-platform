package io.tolgee.dtos

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

data class ImportDto(
        @field:NotEmpty
        val languageAbbreviation: String? = null,

        @field:NotNull
        var data: Map<String, String>? = null
)
