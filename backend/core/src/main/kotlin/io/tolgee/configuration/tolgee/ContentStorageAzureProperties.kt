package io.tolgee.configuration.tolgee

import io.tolgee.model.contentDelivery.AzureBlobConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery.storage.azure")
class ContentStorageAzureProperties : AzureBlobConfig {
  override var connectionString: String? = null
  override var containerName: String? = null

  fun clear() {
    connectionString = null
    containerName = null
  }
}
