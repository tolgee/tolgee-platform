/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

@Configuration
class S3Configuration(private val properties: TolgeeProperties) {

  private val s3config = properties.fileStorage.s3

  @Bean
  fun s3(): S3Client? {
    if (s3config.enabled) {
      val credentialsProvider = getCredentialsProvider()

      val builder = S3Client.builder().credentialsProvider(credentialsProvider).serviceConfiguration(
        S3Configuration.builder().pathStyleAccessEnabled(true).build()
      )
      if (!s3config.endpoint.isNullOrEmpty()) {
        builder.setEndpoint()
      }
      if (!s3config.signingRegion.isNullOrEmpty()) {
        builder.region(Region.of(s3config.signingRegion))
      }
      return builder.build()
    }
    return null
  }

  private fun getCredentialsProvider(): AwsCredentialsProvider? = when (
    s3config.accessKey.isNullOrEmpty() ||
      s3config.secretKey.isNullOrEmpty()
  ) {
    true -> DefaultCredentialsProvider.create()
    false -> StaticCredentialsProvider.create(
      AwsBasicCredentials.create(
        s3config.accessKey, s3config.secretKey
      )
    )
  }

  private fun S3ClientBuilder.setEndpoint() {
    try {
      endpointOverride(URI.create(s3config.endpoint!!))
    } catch (e: NullPointerException) {
      endpointOverride(URI.create("""https://${s3config.endpoint}"""))
    }
  }
}
