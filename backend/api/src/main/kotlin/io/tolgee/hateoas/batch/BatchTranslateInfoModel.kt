package io.tolgee.hateoas.batch

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.LlmProviderType

@Schema(description = "Information about batch translation availability and pricing")
data class BatchTranslateInfoModel(
  @Schema(description = "Whether batch API is available for this project's provider")
  val available: Boolean,
  @Schema(description = "Approximate discount percentage compared to sync pricing, or null if not computable")
  val discountPercent: Double?,
  @Schema(description = "Whether the user is allowed to choose batch mode")
  val userChoiceAllowed: Boolean,
  @Schema(description = "Provider type used for translation")
  val providerType: LlmProviderType?,
)
