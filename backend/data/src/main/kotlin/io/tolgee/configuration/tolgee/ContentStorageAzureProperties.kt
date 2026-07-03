package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.contentDelivery.AzureBlobConfig

@DocProperty(prefix = "tolgee.content-delivery.storage.azure")
class ContentStorageAzureProperties : AzureBlobConfig {
  override var connectionString: String? = null
  override var containerName: String? = null

  fun clear() {
    connectionString = null
    containerName = null
  }
}
