package io.tolgee.dtos.cdn

import javax.validation.Valid

data class CdnStorageDto(
  @field:Valid
  val azureCdnConfig: AzureCdnConfigDto?,
  @field:Valid
  val s3CdnConfig: S3CdnConfigDto?
)
