package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(prefix = "tolgee.content-delivery.cache-purging")
class ContentDeliveryCachePurgingProperties {
  var azureFrontDoor = ContentDeliveryAzureFrontDoorProperties()
  var cloudflare = ContentDeliveryCloudflareProperties()
}
