package io.tolgee.dtos.request.export

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.model.enums.TranslationState

data class ExportParams(
  @field:Parameter(
    description = """Languages to be contained in export.
                
If null, all languages are exported""",
    example = "en"
  )
  var languages: Set<String>? = null,

  @field:Parameter(
    description = """Format to export to""",
  )
  var format: ExportFormat = ExportFormat.JSON,

  @field:Parameter(
    description = """When true translations are split to directories by scopes""",
  )
  var splitByScope: Boolean = false,

  @field:Parameter(
    description = """Scope delimiter.

e.g. For key "home.header.title" scopes would result in "home" -> "header", when splitByScopeDepth is greater than 1.
    """,
  )
  var splitByScopeDelimiter: Char = '.',

  @field:Parameter(
    description = """Maximum depth of scoping.

e.g. For key "home.header.title" and depth 1, resulting scope is  "home".

For depth 2, resulting scopes are  "home" -> "header".
    """,
  )
  var splitByScopeDepth: Int = 1,

  @field:Parameter(
    description = """Filter key IDs to be contained in export""",
  )
  var filterKeyId: List<Long>? = null,

  @field:Parameter(
    description = """Filter key IDs not to be contained in export""",
  )
  var filterKeyIdNot: List<Long>? = null,

  @field:Parameter(
    description = """Filter keys tagged by""",
  )
  var filterTag: String? = null,

  @field:Parameter(
    description = """Filter keys with prefix""",
  )
  var filterKeyPrefix: String? = null,

  @field:Parameter(
    description = """Filter translations with state. By default, everything except untranslated is exported.""",
  )
  var filterState: List<TranslationState>? = listOf(
    TranslationState.TRANSLATED,
    TranslationState.REVIEWED,
  ),

  @field:Parameter(
    description = """If false, it doesn't return zip of files, but it returns single file.
      
This is possible only when single language is exported. Otherwise it returns "400 - Bad Request" response. 
    """
  )
  var zip: Boolean = true
) {
  @get:Hidden
  val shouldContainUntranslated: Boolean
    get() = this.filterState?.contains(TranslationState.UNTRANSLATED) != false
}
