package io.tolgee.dtos.request.key

import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Length
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@Validated
data class CreateKeyDto(
  /**
   * Key full path is stored as name in entity
   */
  @Schema(description = "Name of the key")
  @field:NotBlank
  @field:Length(max = 2000, min = 1)
  val name: String = "",

  val translations: Map<String, String?>? = null,

  val tags: List<String>? = null,

  @Schema(description = "Ids of screenshots uploaded with /v2/image-upload endpoint")
  val screenshotUploadedImageIds: List<Long>? = null
)
