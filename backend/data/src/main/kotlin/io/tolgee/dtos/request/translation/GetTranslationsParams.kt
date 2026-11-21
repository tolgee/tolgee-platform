package io.tolgee.dtos.request.translation

import io.swagger.v3.oas.annotations.Parameter

class GetTranslationsParams(
  // When filterState has setter, it starts to split it's values on ',' char,
  // so we need to add it here, to be able to not provide the setter
  filterState: List<String>? = null,
  @field:Parameter(description = "Cursor to get next data")
  val cursor: String? = null,
  var contextKeyId: Long? = null,
  @field:jakarta.validation.constraints.Min(0)
  @field:jakarta.validation.constraints.Max(10)
  var contextRadius: Int = 2,
  @field:Parameter(description = "Include detailed QA issues for inline highlighting")
  var includeQaIssues: Boolean? = false,
) : TranslationFilters(filterState)
