package io.tolgee.dtos.cdn

import io.tolgee.model.cdn.AzureBlobConfig
import javax.validation.constraints.NotBlank

class AzureCdnConfigDto : AzureBlobConfig {
  override var connectionString: String? = ""

  @field:NotBlank
  override var containerName: String? = ""
}
