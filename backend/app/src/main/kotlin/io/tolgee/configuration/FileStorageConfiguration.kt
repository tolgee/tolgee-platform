/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration

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
) {
  private val s3config = properties.fileStorage.s3

  @Bean
  fun fileStorage(): FileStorage {
    if (properties.internal.useInMemoryFileStorage) {
      return InMemoryFileStorage()
    }
    if (s3config.enabled) {
      return s3FileStorageFactory.create(s3config)
    }
    return LocalFileStorage(tolgeeProperties = properties)
  }
}
