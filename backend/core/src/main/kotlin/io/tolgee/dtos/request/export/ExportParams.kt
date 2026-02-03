package io.tolgee.dtos.request.export

import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.dtos.ExportParamsDocs
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
import io.tolgee.dtos.ExportParamsDocs.MESSAGE_FORMAT_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.STRUCTURE_DELIMITER_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.SUPPORT_ARRAYS_DESCRIPTION
import io.tolgee.dtos.ExportParamsDocs.ZIP_DESCRIPTION
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.model.enums.TranslationState

data class ExportParams(
  @field:Parameter(
    description = ExportParamsDocs.LANGUAGES_DESCRIPTION,
    example = ExportParamsDocs.LANGUAGES_EXAMPLE,
  )
  override var languages: Set<String>? = null,
  @field:Parameter(
    description = FORMAT_DESCRIPTION,
  )
  override var format: ExportFormat = ExportFormat.JSON,
  @field:Parameter(
    description = STRUCTURE_DELIMITER_DESCRIPTION,
  )
  override var structureDelimiter: Char? = '.',
  @field:Parameter(
    description = FILTER_KEY_ID_DESCRIPTION,
  )
  override var filterKeyId: List<Long>? = null,
  @field:Parameter(
    description = FILTER_KEY_ID_NOT_DESCRIPTION,
  )
  override var filterKeyIdNot: List<Long>? = null,
  @field:Parameter(
    description = FILTER_TAG_DESCRIPTION,
  )
  override var filterTag: String? = null,
  @field:Parameter(
    description = FILTER_TAG_IN_DESCRIPTION,
  )
  override var filterTagIn: List<String>? = null,
  @field:Parameter(
    description = FILTER_TAG_NOT_IN_DESCRIPTION,
  )
  override var filterTagNotIn: List<String>? = null,
  @field:Parameter(
    description = FILTER_KEY_PREFIX_DESCRIPTION,
  )
  override var filterKeyPrefix: String? = null,
  @field:Parameter(
    description = FILTER_STATE_DESCRIPTION,
  )
  override var filterState: List<TranslationState>? =
    listOf(
      TranslationState.TRANSLATED,
      TranslationState.REVIEWED,
    ),
  @field:Parameter(
    description = FILTER_NAMESPACE_DESCRIPTION,
  )
  override var filterNamespace: List<String?>? = null,
  @field:Parameter(
    description = ZIP_DESCRIPTION,
  )
  var zip: Boolean = true,
  @field:Parameter(
    description = MESSAGE_FORMAT_DESCRIPTION,
  )
  override var messageFormat: ExportMessageFormat? = null,
  @field:Parameter(
    description = ExportParamsDocs.FILE_STRUCTURE_TEMPLATE_DESCRIPTION,
  )
  override var fileStructureTemplate: String? = null,
  @field:Parameter(
    description = SUPPORT_ARRAYS_DESCRIPTION,
  )
  override var supportArrays: Boolean = false,
  /**
   * Enabling or disabling HTML escaping for XLIFF format. Some tools expect the XML/HTML tags escaped and don't accept raw unescaped tags.
   */
  @field:Parameter(
    description = HTML_ESCAPE_DESCRIPTION,
  )
  override var escapeHtml: Boolean? = false,
) : IExportParams
