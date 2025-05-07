package io.tolgee.configuration.tolgee

import io.tolgee.model.contentDelivery.S3Config
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery.storage.s3")
data class ContentStorageS3Properties(
  override var bucketName: String? = null,
  override var accessKey: String? = null,
  override var secretKey: String? = null,
  override var endpoint: String? = null,
  override var signingRegion: String? = null,
  override var path: String? = null,
) : S3Config {
  fun clear() {
    bucketName = null
    accessKey = null
    secretKey = null
    endpoint = null
    signingRegion = null
    path = null
  }
}
