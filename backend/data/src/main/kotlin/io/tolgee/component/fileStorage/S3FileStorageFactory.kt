package io.tolgee.component.fileStorage

import io.tolgee.model.contentDelivery.S3Config
import io.tolgee.util.SsrfSafeRequestFactoryProvider
import org.springframework.stereotype.Component

@Component
class S3FileStorageFactory(
  private val ssrfSafeRequestFactoryProvider: SsrfSafeRequestFactoryProvider,
) {
  fun create(config: S3Config): S3FileStorage = createStorage(config, S3ClientProvider(config))

  fun createSsrfSafe(
    config: S3Config,
    allowLocalAddresses: Boolean,
  ): S3FileStorage {
    val dnsResolver = ssrfSafeRequestFactoryProvider.validatingResolver(allowLocalAddresses)
    return createStorage(config, S3ClientProvider(config, dnsResolver))
  }

  private fun createStorage(
    config: S3Config,
    clientProvider: S3ClientProvider,
  ): S3FileStorage {
    val bucketName = config.bucketName ?: throw RuntimeException("Bucket name for S3 storage is not set")
    return S3FileStorage(
      bucketName = bucketName,
      path = config.path,
      s3 = clientProvider.provide(),
      publicRead = config.publicRead,
    )
  }
}
