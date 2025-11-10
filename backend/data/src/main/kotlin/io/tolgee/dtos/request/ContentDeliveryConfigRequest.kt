package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.ExportParamsDocs
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.model.enums.TranslationState
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class ContentDeliveryConfigRequest : IExportParams {
  @field:NotBlank
  var name: String = ""

  @Schema(
    description =
      "Id of custom storage to use for content delivery. If null, default server storage is used. " +
        "Tolgee Cloud provides default Content Storage.",
  )
  var contentStorageId: Long? = null

  @Schema(
    description = "If true, data are published to the content delivery automatically after each change.",
  )
  var autoPublish: Boolean = false

  @Schema(
    description =
      "Tolgee uses a custom slug as a directory name for content storage and public content delivery URL. " +
        "It is only applicable for custom storage. " +
        "This field needs to be kept null for Tolgee Cloud content storage or global server storage on " +
        "self-hosted instances.\n\n" +
        "Slag has to match following regular expression: `^[a-z0-9]+(?:-[a-z0-9]+)*\$`.\n\n" +
        "If null is provided for update operation, slug will be assigned with generated value.",
  )
  @field:Size(min = 1, max = 60)
  var slug: String? = null

  @Schema(
    description =
      "Whether the data in the CDN should be pruned before publishing new data.\n\n" +
        "In some cases, you might want to keep the data in the storage and only replace the " +
        "files created by following publish operation.",
  )
  var pruneBeforePublish = true

  override var languages: Set<String>? = null
  override var format: ExportFormat = ExportFormat.JSON
  override var structureDelimiter: Char? = '.'
  override var supportArrays: Boolean = false
  override var filterKeyId: List<Long>? = null

  override var filterKeyIdNot: List<Long>? = null

  override var filterTag: String? = null

  override var filterTagIn: List<String>? = null

  override var filterTagNotIn: List<String>? = null

  override var filterKeyPrefix: String? = null

  override var filterState: List<TranslationState>? =
    listOf(
      TranslationState.TRANSLATED,
      TranslationState.REVIEWED,
    )

  override var filterNamespace: List<String?>? = null

  override var messageFormat: ExportMessageFormat? = null

  override var fileStructureTemplate: String? = null

  @Schema(
    description = ExportParamsDocs.HTML_ESCAPE_DESCRIPTION,
  )
  override var escapeHtml: Boolean? = false
}
