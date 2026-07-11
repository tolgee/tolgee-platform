package io.tolgee.dtos.contentDelivery

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.contentDelivery.S3Config
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class S3ContentStorageConfigDto : S3Config {
  @field:NotBlank
  @field:Size(min = 3, max = 63)
  override var bucketName: String = ""

  @field:Size(max = 255)
  override var accessKey: String? = ""

  @field:Size(max = 255)
  override var secretKey: String? = ""

  @field:NotBlank
  @field:Size(max = 255)
  override var endpoint: String = ""

  @field:NotBlank
  @field:Size(max = 255)
  override val signingRegion: String = ""

  @field:Schema(
    description = "Specifies an optional subfolder structure within s3 bucket to which content will be stored",
  )
  @field:Size(max = 255)
  override val path: String = ""
}
