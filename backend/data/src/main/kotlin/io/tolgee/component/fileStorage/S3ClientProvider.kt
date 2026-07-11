package io.tolgee.component.fileStorage

import io.tolgee.model.contentDelivery.S3Config
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.LegacyMd5Plugin
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
      builder.configureForCustomEndpoint()
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

  private fun S3ClientBuilder.configureForCustomEndpoint() {
    setEndpoint()
    // Custom endpoints point at third-party S3-compatible stores (MinIO, Ceph, ...). Many reject
    // the CRC32 trailer the SDK adds to uploads by default since 2.30. Restore pre-2.30 request
    // behavior: skip the checksum on optional operations, and keep legacy Content-MD5 (not CRC32)
    // on operations that require integrity, such as the multi-object delete that S3FileStorage issues.
    requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
    addPlugin(LegacyMd5Plugin.create())
  }

  private fun S3ClientBuilder.setEndpoint() {
    try {
      endpointOverride(URI.create(s3config.endpoint!!))
    } catch (e: NullPointerException) {
      endpointOverride(URI.create("""https://${s3config.endpoint}"""))
    }
  }
}
