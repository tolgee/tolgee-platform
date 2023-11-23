package io.tolgee.dtos.contentDelivery

import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class ContentStorageRequest(
  @field:NotBlank
  @field:Size(max = 100)
  val name: String,
  @field:Valid
  val azureContentStorageConfig: AzureContentStorageConfigDto?,
  @field:Valid
  val s3ContentStorageConfig: S3ContentStorageConfigDto?,
  @field:Size(max = 255)
  val publicUrlPrefix: String? = null
)
