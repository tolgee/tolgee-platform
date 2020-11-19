package io.polygloat.controllers

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

data class ImportDto(
        val languageAbbreviation: @NotEmpty String? = null,
        var data: @NotNull Map<String, String>? = null
)