package io.tolgee.component.fileStorage

import io.tolgee.model.contentDelivery.S3Config
import io.tolgee.util.SsrfSafeRequestFactoryProvider
import org.apache.hc.client5.http.DnsResolver
import org.springframework.stereotype.Component

@Component
class S3FileStorageFactory(
  private val ssrfSafeRequestFactoryProvider: SsrfSafeRequestFactoryProvider,
) {
  fun create(
    config: S3Config,
    allowLocalAddresses: Boolean,
  ): S3FileStorage {
    val client = S3ClientProvider(config, dnsResolver(allowLocalAddresses)).provide()
    val bucketName = config.bucketName ?: throw RuntimeException("Bucket name for S3 storage is not set")
    return S3FileStorage(bucketName = bucketName, path = config.path, s3 = client, publicRead = config.publicRead)
  }

  private fun dnsResolver(allowLocalAddresses: Boolean): DnsResolver? {
    if (allowLocalAddresses) return null
    return ssrfSafeRequestFactoryProvider.validatingResolver(allowLocalAddresses = false)
  }
}
