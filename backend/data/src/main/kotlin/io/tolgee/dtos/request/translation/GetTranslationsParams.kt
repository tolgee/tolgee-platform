package io.tolgee.dtos.request.translation

import io.swagger.v3.oas.annotations.Parameter

class GetTranslationsParams(
  // When filterState has setter, it starts to split it's values on ',' char,
  // so we need to add it here, to be able to not provide the setter
  filterState: List<String>? = null,
  @field:Parameter(description = "Cursor to get next data")
  val cursor: String? = null,
) : TranslationFilters(filterState)
