package io.tolgee.configuration.tolgee

import io.tolgee.model.contentDelivery.AzureFrontDoorConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery.cache-purging.azure-front-door")
class ContentDeliveryAzureFrontDoorProperties : AzureFrontDoorConfig {
  override var clientId: String? = null
  override var clientSecret: String? = null
  override var tenantId: String? = null
  override var contentRoot: String? = null
  override var subscriptionId: String? = null
  override var endpointName: String? = null
  override var profileName: String? = null
  override var resourceGroupName: String? = null
}
