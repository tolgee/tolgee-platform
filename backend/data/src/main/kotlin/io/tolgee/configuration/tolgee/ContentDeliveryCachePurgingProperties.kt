package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.NestedConfigurationProperty

@DocProperty(prefix = "tolgee.content-delivery.cache-purging")
class ContentDeliveryCachePurgingProperties {
  @NestedConfigurationProperty
  var azureFrontDoor = ContentDeliveryAzureFrontDoorProperties()

  @NestedConfigurationProperty
  var cloudflare = ContentDeliveryCloudflareProperties()
}
