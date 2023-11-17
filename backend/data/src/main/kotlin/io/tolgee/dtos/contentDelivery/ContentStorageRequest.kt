package io.tolgee.dtos.contentDelivery

import javax.validation.Valid
import javax.validation.constraints.NotBlank

data class ContentStorageRequest(
  @NotBlank
  val name: String,
  @field:Valid
  val azureContentStorageConfig: AzureContentStorageConfigDto?,
  @field:Valid
  val s3ContentStorageConfig: S3ContentStorageConfigDto?,
  val publicUrlPrefix: String? = null
)
