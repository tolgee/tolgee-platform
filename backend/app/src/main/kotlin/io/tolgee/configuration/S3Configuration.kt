/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration

import io.tolgee.component.fileStorage.S3ClientProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class S3Configuration(private val properties: TolgeeProperties) {

  private val s3config = properties.fileStorage.s3

  @Bean
  fun s3(): S3Client? {
    if (s3config.enabled) {
      S3ClientProvider(s3config).provide()
    }
    return null
  }
}
