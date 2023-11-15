package io.tolgee.component.cdn

import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.cdn.AzureBlobConfig
import io.tolgee.model.cdn.S3Config
import io.tolgee.model.cdn.StorageConfig
import org.springframework.stereotype.Component

@Component
class CdnFileStorageProvider(
  private val tolgeeProperties: TolgeeProperties,
  private val s3FileStorageFactory: S3FileStorageFactory,
  private val azureFileStorageFactory: AzureFileStorageFactory
) {
  fun getCdnStorageWithDefaultClient(): FileStorage {
    return defaultStorage
  }

  fun getStorage(config: StorageConfig): FileStorage {
    return when (config) {
      is AzureBlobConfig -> {
        azureFileStorageFactory.create(config)
      }

      is S3Config -> {
        s3FileStorageFactory.create(config)
      }

      else -> {
        throw Exception("Unknown storage config")
      }
    }
  }

  val defaultStorage by lazy {
    val props = getDefaultStorageProperties()
    getStorage(props)
  }

  fun getDefaultStorageProperties(): StorageConfig {
    val isSingleSet =
      (tolgeeProperties.cdn.storage.s3.enabled) xor (tolgeeProperties.cdn.storage.azure.enabled)

    if (!isSingleSet) {
      throw RuntimeException("Exactly one of CDN storages must be set")
    }

    if (tolgeeProperties.cdn.storage.s3.enabled) {
      return tolgeeProperties.cdn.storage.s3
    }

    if (tolgeeProperties.cdn.storage.azure.enabled) {
      return tolgeeProperties.cdn.storage.azure
    }

    throw RuntimeException("No CDN storage is set")
  }
}
