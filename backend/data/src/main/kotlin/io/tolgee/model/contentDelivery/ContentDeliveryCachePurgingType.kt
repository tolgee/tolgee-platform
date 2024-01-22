package io.tolgee.model.contentDelivery

import io.tolgee.component.contentDelivery.cachePurging.AzureContentDeliveryCachePurgingFactory
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingFactory
import kotlin.reflect.KClass

enum class ContentDeliveryCachePurgingType(val factory: KClass<out ContentDeliveryCachePurgingFactory>) {
  AZURE_FRONT_DOOR(AzureContentDeliveryCachePurgingFactory::class),
}
