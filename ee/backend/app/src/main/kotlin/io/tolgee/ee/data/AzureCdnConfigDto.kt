package io.tolgee.ee.data

import io.tolgee.model.cdn.AzureBlobConfig
import javax.validation.constraints.NotBlank

class AzureCdnConfigDto : AzureBlobConfig {
  @field:NotBlank
  override var connectionString: String? = ""

  @field:NotBlank
  override var containerName: String? = ""
}
