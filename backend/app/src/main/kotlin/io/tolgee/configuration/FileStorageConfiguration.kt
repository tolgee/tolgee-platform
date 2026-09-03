/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration

import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.LocalFileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.util.InMemoryFileStorage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FileStorageConfiguration(
  private val properties: TolgeeProperties,
  private val s3FileStorageFactory: S3FileStorageFactory,
  private val azureFileStorageFactory: AzureFileStorageFactory,
) {
  private val s3config = properties.fileStorage.s3
  private val azureConfig = properties.fileStorage.azure

  @Bean
  fun fileStorage(): FileStorage {
    if (properties.internal.useInMemoryFileStorage) {
      return InMemoryFileStorage()
    }
    check(!(s3config.enabled && azureConfig.enabled)) {
      "Both tolgee.file-storage.s3 and tolgee.file-storage.azure are enabled. Configure exactly one file storage."
    }
    if (azureConfig.enabled) {
      check(!azureConfig.connectionString.isNullOrBlank() && !azureConfig.containerName.isNullOrBlank()) {
        "tolgee.file-storage.azure is enabled, but connection-string or container-name is not set."
      }
      return azureFileStorageFactory.create(azureConfig)
    }
    if (s3config.enabled) {
      return s3FileStorageFactory.create(s3config)
    }
    return LocalFileStorage(tolgeeProperties = properties)
  }
}
