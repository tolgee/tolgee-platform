/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration

import com.amazonaws.services.s3.AmazonS3
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.LocalFileStorage
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FileStorageConfiguration(
  private val properties: TolgeeProperties,
  private val amazonS3: AmazonS3?
) {

  private val s3config = properties.fileStorage.s3

  @Bean
  fun fileStorage(): FileStorage {
    if (s3config.enabled && amazonS3 != null) {
      return S3FileStorage(tolgeeProperties = properties, amazonS3)
    }
    return LocalFileStorage(tolgeeProperties = properties)
  }
}
