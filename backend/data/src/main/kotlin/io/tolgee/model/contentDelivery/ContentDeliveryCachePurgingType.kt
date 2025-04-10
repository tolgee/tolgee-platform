package io.tolgee.model.contentDelivery

import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingFactory
import io.tolgee.component.contentDelivery.cachePurging.awsCloudFront.AWSCloudFrontContentDeliveryCachePurgingFactory
import io.tolgee.component.contentDelivery.cachePurging.azureFrontDoor.AzureContentDeliveryCachePurgingFactory
import io.tolgee.component.contentDelivery.cachePurging.bunny.BunnyContentDeliveryCachePurgingFactory
import io.tolgee.component.contentDelivery.cachePurging.cloudflare.CloudflareContentDeliveryCachePurgingFactory
import kotlin.reflect.KClass

enum class ContentDeliveryCachePurgingType(
  val factory: KClass<out ContentDeliveryCachePurgingFactory>,
) {
  AZURE_FRONT_DOOR(AzureContentDeliveryCachePurgingFactory::class),
  AWS_CLOUD_FRONT(AWSCloudFrontContentDeliveryCachePurgingFactory::class),
  CLOUDFLARE(CloudflareContentDeliveryCachePurgingFactory::class),
  BUNNY(BunnyContentDeliveryCachePurgingFactory::class),
}
