package io.tolgee.component.fileStorage

import io.tolgee.model.contentDelivery.S3Config
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

class S3ClientProvider(
  private val s3config: S3Config,
) {
  fun provide(): S3Client {
    val credentialsProvider = getCredentialsProvider()

    val builder =
      S3Client.builder().credentialsProvider(credentialsProvider).serviceConfiguration(
        S3Configuration.builder().pathStyleAccessEnabled(true).build(),
      )
    if (!s3config.endpoint.isNullOrEmpty()) {
      builder.setEndpoint()
    }
    if (!s3config.signingRegion.isNullOrEmpty()) {
      builder.region(Region.of(s3config.signingRegion))
    }
    return builder.build()
  }

  private fun getCredentialsProvider(): AwsCredentialsProvider? =
    when (
      s3config.accessKey.isNullOrEmpty() ||
        s3config.secretKey.isNullOrEmpty()
    ) {
      true -> DefaultCredentialsProvider.create()
      false ->
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create(
            s3config.accessKey,
            s3config.secretKey,
          ),
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
