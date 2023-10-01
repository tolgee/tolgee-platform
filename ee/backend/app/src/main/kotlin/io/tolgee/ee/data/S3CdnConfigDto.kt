package io.tolgee.ee.data

import io.tolgee.model.cdn.S3Config
import javax.validation.constraints.NotBlank

class S3CdnConfigDto : S3Config {
  @field:NotBlank
  override var bucketName: String = ""

  @field:NotBlank
  override var accessKey: String = ""

  @field:NotBlank
  override var secretKey: String = ""

  @field:NotBlank
  override var endpoint: String = ""

  @field:NotBlank
  override val signingRegion: String = ""
}
