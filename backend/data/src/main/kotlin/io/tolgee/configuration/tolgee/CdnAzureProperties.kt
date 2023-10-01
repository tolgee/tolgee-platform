package io.tolgee.configuration.tolgee

import io.tolgee.model.cdn.AzureBlobConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.cdn.azure")
class CdnAzureProperties : AzureBlobConfig {
  override var connectionString: String? = null
  override var containerName: String? = null
}
