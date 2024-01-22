package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.model.enums.TranslationState
import jakarta.validation.constraints.NotBlank

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

  override var languages: Set<String>? = null
  override var format: ExportFormat = ExportFormat.JSON
  override var structureDelimiter: Char? = '.'

  override var filterKeyId: List<Long>? = null

  override var filterKeyIdNot: List<Long>? = null

  override var filterTag: String? = null

  override var filterKeyPrefix: String? = null

  override var filterState: List<TranslationState>? =
    listOf(
      TranslationState.TRANSLATED,
      TranslationState.REVIEWED,
    )

  override var filterNamespace: List<String?>? = null
}
