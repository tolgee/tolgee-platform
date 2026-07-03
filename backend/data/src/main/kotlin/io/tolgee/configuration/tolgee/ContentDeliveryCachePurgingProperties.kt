package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.contentDelivery.ContentDeliveryPurgingConfig
import org.springframework.boot.context.properties.NestedConfigurationProperty

@DocProperty(prefix = "tolgee.content-delivery.cache-purging")
class ContentDeliveryCachePurgingProperties {
  @NestedConfigurationProperty
  var azureFrontDoor = ContentDeliveryAzureFrontDoorProperties()

  @NestedConfigurationProperty
  var cloudflare = ContentDeliveryCloudflareProperties()

  @NestedConfigurationProperty
  var bunny = ContentDeliveryBunnyProperties()

  @NestedConfigurationProperty
  var awsCloudFront = ContentDeliveryAWSCloudFrontProperties()
}

fun ContentDeliveryCachePurgingProperties.all(): List<ContentDeliveryPurgingConfig> =
  listOf(azureFrontDoor, cloudflare, bunny, awsCloudFront)
