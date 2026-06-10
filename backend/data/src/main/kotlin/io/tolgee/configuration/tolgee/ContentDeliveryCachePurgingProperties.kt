package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery.cache-purging")
class ContentDeliveryCachePurgingProperties {
  var azureFrontDoor = ContentDeliveryAzureFrontDoorProperties()
  var cloudflare = ContentDeliveryCloudflareProperties()
  var awsCloudfront = ContentDeliveryAWSCloudFrontProperties()
  var bunny = ContentDeliveryBunnyProperties()
}
