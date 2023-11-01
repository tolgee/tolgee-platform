package io.tolgee.dtos.cdn

import javax.validation.Valid
import javax.validation.constraints.NotBlank

data class CdnStorageDto(
  @NotBlank
  val name: String,
  @field:Valid
  val azureCdnConfig: AzureCdnConfigDto?,
  @field:Valid
  val s3CdnConfig: S3CdnConfigDto?
)
