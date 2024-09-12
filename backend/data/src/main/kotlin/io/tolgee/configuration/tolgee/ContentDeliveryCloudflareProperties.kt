package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.contentDelivery.ContentDeliveryCachePurgingType
import io.tolgee.model.contentDelivery.ContentDeliveryPurgingConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery.cache-purging.cloudflare")
class ContentDeliveryCloudflareProperties(
  var apiKey: String? = null,
  var urlPrefix: String? = null,
  var zoneId: String? = null,
  @DocProperty(
    "Number of paths to purge in one request. " +
      "(Cloudflare limit is 30 now, but it might be subject to change)",
  )
  var maxFilesPerRequest: Int = 30,
) : ContentDeliveryPurgingConfig {
  override val enabled: Boolean
    get() = !apiKey.isNullOrEmpty() && !urlPrefix.isNullOrEmpty() && !zoneId.isNullOrEmpty()

  override val contentDeliveryCachePurgingType: ContentDeliveryCachePurgingType
    get() = ContentDeliveryCachePurgingType.CLOUDFLARE
}
