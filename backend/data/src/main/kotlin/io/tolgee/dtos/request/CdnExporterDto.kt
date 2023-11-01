package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.model.enums.TranslationState
import javax.validation.constraints.NotBlank

class CdnExporterDto : IExportParams {
  @field:NotBlank
  var name: String = ""

  var cdnStorageId: Long? = null

  @field:Parameter(
    description = """Languages to be contained in export.
                
If null, all languages are exported""",
    example = "en"
  )
  override var languages: Set<String>? = null

  @field:Parameter(
    description = """Format to export to""",
  )
  override var format: ExportFormat = ExportFormat.JSON

  @field:Parameter(
    description = """Delimiter to structure file content. 

e.g. For key "home.header.title" would result in {"home": {"header": "title": {"Hello"}}} structure.

When null, resulting file won't be structured.
    """,
  )
  override var structureDelimiter: Char? = '.'

  @field:Parameter(
    description = """Filter key IDs to be contained in export""",
  )
  override var filterKeyId: List<Long>? = null

  @field:Parameter(
    description = """Filter key IDs not to be contained in export""",
  )
  override var filterKeyIdNot: List<Long>? = null

  @field:Parameter(
    description = """Filter keys tagged by""",
  )
  override var filterTag: String? = null

  @field:Parameter(
    description = """Filter keys with prefix""",
  )
  override var filterKeyPrefix: String? = null

  @field:Parameter(
    description = """Filter translations with state. By default, everything except untranslated is exported.""",
  )
  override var filterState: List<TranslationState>? = listOf(
    TranslationState.TRANSLATED,
    TranslationState.REVIEWED,
  )

  @field:Parameter(
    description = """Select one ore multiple namespaces to export"""
  )
  override var filterNamespace: List<String?>? = null
}
