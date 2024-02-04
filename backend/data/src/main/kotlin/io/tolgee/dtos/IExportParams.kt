package io.tolgee.dtos

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.model.enums.TranslationState

interface IExportParams {
  @get:Schema(
    description = """Languages to be contained in export.
                
If null, all languages are exported""",
    example = "en",
  )
  var languages: Set<String>?

  @get:Schema(
    description = """Format to export to""",
  )
  var format: ExportFormat

  @get:Schema(
    description = """Delimiter to structure file content. 

e.g. For key "home.header.title" would result in {"home": {"header": "title": {"Hello"}}} structure.

When null, resulting file won't be structured.
    """,
  )
  var structureDelimiter: Char?

  @get:Schema(
    description = """Filter key IDs to be contained in export""",
  )
  var filterKeyId: List<Long>?

  @get:Schema(
    description = """Filter key IDs not to be contained in export""",
  )
  var filterKeyIdNot: List<Long>?

  @get:Schema(
    description = """Filter keys tagged by""",
  )
  var filterTag: String?

  @get:Schema(
    description = """Filter keys with prefix""",
  )
  var filterKeyPrefix: String?

  @get:Schema(
    description = """Filter translations with state. By default, everything except untranslated is exported.""",
  )
  var filterState: List<TranslationState>?

  @get:Schema(
    description = """Select one ore multiple namespaces to export""",
  )
  var filterNamespace: List<String?>?

  @get:Hidden
  @get:JsonIgnore
  val shouldContainUntranslated: Boolean
    get() = this.filterState?.contains(TranslationState.UNTRANSLATED) != false

  @get:Schema(
    description = """Message format to be used for export. (applicable for .po)
      
e.g. PHP_PO: Hello %s, PYTHON_PO: Hello %(name)s   
    """,
  )
  var messageFormat: ExportMessageFormat?

  fun copyPropsFrom(other: IExportParams) {
    this.languages = other.languages
    this.format = other.format
    this.structureDelimiter = other.structureDelimiter
    this.filterKeyId = other.filterKeyId
    this.filterKeyIdNot = other.filterKeyIdNot
    this.filterTag = other.filterTag
    this.filterKeyPrefix = other.filterKeyPrefix
    this.filterState = other.filterState
    this.filterNamespace = other.filterNamespace
    this.messageFormat = other.messageFormat
  }
}
