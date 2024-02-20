package io.tolgee.dtos.request.export

import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.model.enums.TranslationState

data class ExportParams(
  @field:Parameter(
    description = """Languages to be contained in export.
                
If null, all languages are exported""",
    example = "en",
  )
  override var languages: Set<String>? = null,
  @field:Parameter(
    description = """Format to export to""",
  )
  override var format: ExportFormat = ExportFormat.JSON,
  @field:Parameter(
    description = """Delimiter to structure file content. 

e.g. For key "home.header.title" would result in {"home": {"header": "title": {"Hello"}}} structure.

When null, resulting file won't be structured.
    """,
  )
  override var structureDelimiter: Char? = '.',
  @field:Parameter(
    description = """Filter key IDs to be contained in export""",
  )
  override var filterKeyId: List<Long>? = null,
  @field:Parameter(
    description = """Filter key IDs not to be contained in export""",
  )
  override var filterKeyIdNot: List<Long>? = null,
  @field:Parameter(
    description = """Filter keys tagged by""",
  )
  override var filterTag: String? = null,
  @field:Parameter(
    description = """Filter keys with prefix""",
  )
  override var filterKeyPrefix: String? = null,
  @field:Parameter(
    description = """Filter translations with state. By default, everything except untranslated is exported.""",
  )
  override var filterState: List<TranslationState>? =
    listOf(
      TranslationState.TRANSLATED,
      TranslationState.REVIEWED,
    ),
  @field:Parameter(
    description = """Select one ore multiple namespaces to export""",
  )
  override var filterNamespace: List<String?>? = null,
  @field:Parameter(
    description = """If false, it doesn't return zip of files, but it returns single file.
      
This is possible only when single language is exported. Otherwise it returns "400 - Bad Request" response. 
    """,
  )
  var zip: Boolean = true,
  @field:Parameter(
    description = """Message format to be used for export. (applicable for .po)
      
e.g. PHP_PO: Hello %s, PYTHON_PO: Hello %(name)s   
    """,
  )
  override var messageFormat: ExportMessageFormat? = null,
) : IExportParams {
  override var supportJsonArrays: Boolean = false
}
