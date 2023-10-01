package io.tolgee.component.cdn

import com.azure.storage.blob.BlobServiceClientBuilder
import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.S3ClientProvider
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.cdn.AzureBlobConfig
import io.tolgee.model.cdn.S3Config
import io.tolgee.model.cdn.StorageConfig
import org.springframework.stereotype.Component

@Component
class CdnFileStorageProvider(
  private val tolgeeProperties: TolgeeProperties,
) {
  fun getCdnStorageWithDefaultClient(): FileStorage {
    return defaultStorage
  }

  fun getStorage(config: StorageConfig): FileStorage {
    return when (config) {
      is AzureBlobConfig -> {
        getAzureStorage(config)
      }
      is S3Config -> {
        getS3Storage(config)
      }
      else -> {
        throw Exception("Unknown storage config")
      }
    }
  }

  private fun getAzureStorage(config: AzureBlobConfig): AzureBlobFileStorage {
    val connectionString = config.connectionString
    val blobServiceClient = BlobServiceClientBuilder()
      .connectionString(connectionString)
      .buildClient()
    val containerClient = blobServiceClient.getBlobContainerClient(config.containerName)
    return AzureBlobFileStorage(containerClient)
  }

  private fun getS3Storage(config: S3Config): S3FileStorage {
    val client = S3ClientProvider(config).provide()
    val bucketName = config.bucketName ?: throw RuntimeException("Bucket name for S3 storage is not set")
    return S3FileStorage(bucketName, client)
  }

  val defaultStorage by lazy {
    val props = getDefaultStorageProperties()
    getStorage(props)
  }

  fun getDefaultStorageProperties(): StorageConfig {
    val isSingleSet =
      (tolgeeProperties.cdn.s3.enabled) xor (tolgeeProperties.cdn.azure.enabled)
    if (!isSingleSet) {
      throw RuntimeException("Exactly one of CDN storages must be set")
    }

    if (tolgeeProperties.cdn.s3.enabled) {
      return tolgeeProperties.cdn.s3
    }

    if (tolgeeProperties.cdn.azure.enabled) {
      return tolgeeProperties.cdn.azure
    }

    throw RuntimeException("No CDN storage is set")
  }
}
