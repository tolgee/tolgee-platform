package io.tolgee.component.contentDelivery

import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.exceptions.FileStoreException
import io.tolgee.model.contentDelivery.AzureBlobConfig
import io.tolgee.model.contentDelivery.S3Config
import io.tolgee.model.contentDelivery.StorageConfig
import org.springframework.stereotype.Component

@Component
class ContentDeliveryFileStorageProvider(
  private val tolgeeProperties: TolgeeProperties,
  private val s3FileStorageFactory: S3FileStorageFactory,
  private val azureFileStorageFactory: AzureFileStorageFactory,
) {
  fun getContentStorageWithDefaultClient(): FileStorage {
    return bypassForTesting() ?: defaultStorage
  }

  fun getStorage(config: StorageConfig): FileStorage {
    return bypassForTesting() ?: when (config) {
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
      (tolgeeProperties.contentDelivery.storage.s3.enabled) xor (tolgeeProperties.contentDelivery.storage.azure.enabled)

    if (!isSingleSet) {
      throw RuntimeException("You have to configure exactly one content storage via configuration properties")
    }

    if (tolgeeProperties.contentDelivery.storage.s3.enabled) {
      return tolgeeProperties.contentDelivery.storage.s3
    }

    if (tolgeeProperties.contentDelivery.storage.azure.enabled) {
      return tolgeeProperties.contentDelivery.storage.azure
    }

    throw RuntimeException("No Content Storage is set")
  }

  fun isServerContentDeliveryConfigured(): Boolean {
    if (tolgeeProperties.contentDelivery.publicUrlPrefix.isNullOrEmpty()) {
      return false
    }

    return try {
      getDefaultStorageProperties()
      true
    } catch (e: Throwable) {
      false
    }
  }

  private fun bypassForTesting(): FileStorage? {
    if (tolgeeProperties.internal.e3eContentStorageBypassOk == null) {
      return null
    }

    val shouldBeOk = tolgeeProperties.internal.e3eContentStorageBypassOk!!

    if (shouldBeOk) {
      return okFileStorage
    }

    return failingFileStorage
  }

  private val okFileStorage
    get() =
      object : FileStorage {
        override fun readFile(storageFilePath: String): ByteArray {
          return ByteArray(0)
        }

        override fun deleteFile(storageFilePath: String) {}

        override fun storeFile(
          storageFilePath: String,
          bytes: ByteArray,
        ) {
        }

        override fun fileExists(storageFilePath: String): Boolean = true

        override fun pruneDirectory(path: String) {}
      }

  private val failingFileStorage
    get() =
      object : FileStorage {
        override fun readFile(storageFilePath: String): ByteArray {
          throw FileStoreException("Bypassed storage get exception", "test", IllegalStateException())
        }

        override fun deleteFile(storageFilePath: String) {
          throw FileStoreException("Bypassed storage delete exception", "test", IllegalStateException())
        }

        override fun storeFile(
          storageFilePath: String,
          bytes: ByteArray,
        ) {
          throw FileStoreException("Bypassed storage put exception", "test", IllegalStateException())
        }

        override fun fileExists(storageFilePath: String): Boolean {
          throw FileStoreException("Bypassed storage exists exception", "test", IllegalStateException())
        }

        override fun pruneDirectory(path: String) {}

        override fun test() {
          throw FileStoreException("Bypassed storage test exception", "test", IllegalStateException())
        }
      }
}
