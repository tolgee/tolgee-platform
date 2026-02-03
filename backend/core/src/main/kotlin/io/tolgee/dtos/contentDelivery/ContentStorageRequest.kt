package io.tolgee.dtos.contentDelivery

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ContentStorageRequest(
  @field:NotBlank
  @field:Size(max = 100)
  val name: String,
  @field:Valid
  val azureContentStorageConfig: AzureContentStorageConfigDto?,
  @field:Valid
  val s3ContentStorageConfig: S3ContentStorageConfigDto?,
  @field:Size(max = 255)
  val publicUrlPrefix: String? = null,
)
