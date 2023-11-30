package io.tolgee.dtos.contentDelivery

import io.tolgee.model.contentDelivery.AzureBlobConfig
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class AzureContentStorageConfigDto : AzureBlobConfig {
  @field:Size(max = 255)
  override var connectionString: String? = ""

  @field:NotBlank
  @field:Size(max = 255)
  override var containerName: String? = ""
}
