/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.LocalFileStorage
import io.tolgee.component.fileStorage.S3ClientProvider
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.util.InMemoryFileStorage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FileStorageConfiguration(
  private val properties: TolgeeProperties,
) {
  private val s3config = properties.fileStorage.s3

  @Bean
  fun fileStorage(): FileStorage {
    if (properties.internal.useInMemoryFileStorage) {
      return InMemoryFileStorage()
    }
    if (s3config.enabled) {
      val amazonS3 = S3ClientProvider(s3config).provide()
      val bucketName = properties.fileStorage.s3.bucketName ?: throw RuntimeException("Bucket name is not set")
      return S3FileStorage(bucketName, amazonS3)
    }
    return LocalFileStorage(tolgeeProperties = properties)
  }
}
