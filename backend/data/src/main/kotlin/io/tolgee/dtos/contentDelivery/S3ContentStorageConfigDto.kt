package io.tolgee.dtos.contentDelivery

import io.tolgee.model.contentDelivery.S3Config
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class S3ContentStorageConfigDto : S3Config {
  @field:NotBlank
  @field:Size(max = 255)
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
}
