package io.tolgee.dtos.request.key

import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Length
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@Validated
data class ComplexEditKeyDto(
  @Schema(description = "Name of the key")
  @field:NotBlank
  @field:Length(max = 200, min = 1)
  val name: String = "",

  @Schema(description = "Translations to update")
  val translations: Map<String, String?>? = null,

  @Schema(description = "Tags of the key. If not provided tags won't be modified")
  val tags: List<String>? = null,

  @Schema(description = "IDs of screenshots to delete")
  val screenshotIdsToDelete: List<Long>? = null,

  @Schema(description = "Ids of screenshots uploaded with /v2/image-upload endpoint")
  val screenshotUploadedImageIds: List<Long>? = null
)
