package io.tolgee.dtos.contentDelivery

import io.tolgee.model.contentDelivery.AzureBlobConfig
import javax.validation.constraints.NotBlank

class AzureContentStorageConfigDto : AzureBlobConfig {
  override var connectionString: String? = ""

  @field:NotBlank
  override var containerName: String? = ""
}
