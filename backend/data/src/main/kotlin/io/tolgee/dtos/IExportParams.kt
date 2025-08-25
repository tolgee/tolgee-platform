package io.tolgee.dtos

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.ExportParamsDocs.FILE_STRUCTURE_TEMPLATE_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.FILTER_KEY_ID_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.FILTER_KEY_ID_NOT_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.FILTER_KEY_PREFIX_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.FILTER_NAMESPACE_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.FILTER_STATE_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.FILTER_TAG_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.FILTER_TAG_IN_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.FILTER_TAG_NOT_IN_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.FORMAT_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.HTML_ESCAPE_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.LANGUAGES_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.LANGUAGES_EXAMPLE
import io.tolgee.dtos.ExportParamsDocs.MESSAGE_FORMAT_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.STRUCTURE_DELIMITER_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.SUPPORT_ARRAYS_DESCRIPTION
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.model.enums.TranslationState

interface IExportParams {
  @get:Schema(
    description = LANGUAGES_DESCRIPTION,
    example = LANGUAGES_EXAMPLE,
  )
  var languages: Set<String>?

  @get:Schema(
    description = FORMAT_DESCRIPTION,
  )
  var format: ExportFormat

  @get:Schema(
    description = STRUCTURE_DELIMITER_DESCRIPTION,
  )
  var structureDelimiter: Char?

  @get:Schema(
    description = SUPPORT_ARRAYS_DESCRIPTION,
  )
  var supportArrays: Boolean

  @get:Schema(
    description = FILTER_KEY_ID_DESCRIPTION,
  )
  var filterKeyId: List<Long>?

  @get:Schema(
    description = FILTER_KEY_ID_NOT_DESCRIPTION,
  )
  var filterKeyIdNot: List<Long>?

  @get:Schema(
    description = FILTER_TAG_DESCRIPTION,
  )
  var filterTag: String?

  @get:Schema(
    description = FILTER_TAG_IN_DESCRIPTION,
  )
  var filterTagIn: List<String>?

  @get:Schema(
    description = FILTER_TAG_NOT_IN_DESCRIPTION,
  )
  var filterTagNotIn: List<String>?

  @get:Schema(
    description = FILTER_KEY_PREFIX_DESCRIPTION,
  )
  var filterKeyPrefix: String?

  @get:Schema(
    description = FILTER_STATE_DESCRIPTION,
  )
  var filterState: List<TranslationState>?

  @get:Schema(
    description = FILTER_NAMESPACE_DESCRIPTION,
  )
  var filterNamespace: List<String?>?

  @get:Schema(
    description = MESSAGE_FORMAT_DESCRIPTION,
  )
  var messageFormat: ExportMessageFormat?

  @get:Schema(
    description = FILE_STRUCTURE_TEMPLATE_DESCRIPTION,
  )
  var fileStructureTemplate: String?

  @get:Schema(
    description = HTML_ESCAPE_DESCRIPTION,
  )
  var escapeHtml: Boolean?

  fun copyPropsFrom(other: IExportParams) {
    this.languages = other.languages
    this.format = other.format
    this.structureDelimiter = other.structureDelimiter
    this.filterKeyId = other.filterKeyId
    this.filterKeyIdNot = other.filterKeyIdNot
    this.filterTag = other.filterTag
    this.filterTagIn = other.filterTagIn
    this.filterTagNotIn = other.filterTagNotIn
    this.filterKeyPrefix = other.filterKeyPrefix
    this.filterState = other.filterState
    this.filterNamespace = other.filterNamespace
    this.messageFormat = other.messageFormat
    this.supportArrays = other.supportArrays
    this.fileStructureTemplate = other.fileStructureTemplate
    this.escapeHtml = other.escapeHtml
  }

  @get:Hidden
  @get:JsonIgnore
  val shouldContainUntranslated: Boolean
    get() = this.filterState?.contains(TranslationState.UNTRANSLATED) != false
}
