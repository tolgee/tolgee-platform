package io.tolgee.unit.component.fileStorage

import io.tolgee.component.fileStorage.S3ClientProvider
import io.tolgee.configuration.tolgee.ContentStorageS3Properties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation

class S3ClientProviderTest {
  // The SDK resolves the native request-checksum default from this system property (and, below it,
  // the AWS_REQUEST_CHECKSUM_CALCULATION env var and ~/.aws/config). Pin it to the SDK's own default
  // so the native-path assertion is deterministic on CI runners / EKS pods that inject AWS config.
  // The custom-endpoint path sets the value explicitly on the builder, which overrides this.
  private val checksumProperty = "aws.requestChecksumCalculation"
  private var originalChecksumProperty: String? = null

  @BeforeEach
  fun pinChecksumDefault() {
    originalChecksumProperty = System.getProperty(checksumProperty)
    System.setProperty(checksumProperty, "when_supported")
  }

  @AfterEach
  fun restoreChecksumDefault() {
    val original = originalChecksumProperty
    if (original == null) {
      System.clearProperty(checksumProperty)
      return
    }
    System.setProperty(checksumProperty, original)
  }

  @Test
  fun `forces request checksums to WHEN_REQUIRED for custom endpoints so third-party stores accept uploads`() {
    val clientConfig =
      S3ClientProvider(config(endpoint = "http://localhost:29090")).provide().serviceClientConfiguration()

    assertThat(clientConfig.requestChecksumCalculation()).isEqualTo(RequestChecksumCalculation.WHEN_REQUIRED)
  }

  @Test
  fun `leaves request checksums at the SDK default for native AWS S3`() {
    val clientConfig = S3ClientProvider(config(endpoint = null)).provide().serviceClientConfiguration()

    assertThat(clientConfig.requestChecksumCalculation()).isEqualTo(RequestChecksumCalculation.WHEN_SUPPORTED)
  }

  @Test
  fun `applies the legacy MD5 interceptor to the custom-endpoint path only`() {
    val addedForCustomEndpoint =
      interceptorClassNames(endpoint = "http://localhost:29090") - interceptorClassNames(endpoint = null).toSet()

    // addedForCustomEndpoint is what LegacyMd5Plugin installs (the set difference vs the native
    // client), so by construction it is absent from the native path. "LegacyMd5" is an SDK-internal
    // class name: if a future SDK renames it, this still-non-empty set stops matching and the test
    // fails loudly — rather than a bare native `noneMatch` silently passing on the rename.
    assertThat(addedForCustomEndpoint).isNotEmpty().anyMatch { it.contains("LegacyMd5") }
  }

  @Test
  fun `resolves the default credentials chain (EKS Pod Identity) when no static keys are configured`() {
    val config = config(endpoint = null, accessKey = null, secretKey = null)
    val clientConfig = S3ClientProvider(config).provide().serviceClientConfiguration()

    assertThat(clientConfig.credentialsProvider()).isInstanceOf(DefaultCredentialsProvider::class.java)
  }

  @Test
  fun `uses static credentials for the custom-endpoint shape self-hosted users ship`() {
    val clientConfig =
      S3ClientProvider(config(endpoint = "http://localhost:29090")).provide().serviceClientConfiguration()

    assertThat(clientConfig.credentialsProvider()).isInstanceOf(StaticCredentialsProvider::class.java)
  }

  @Test
  fun `falls back to the default credentials chain when only one static key is configured`() {
    val clientConfig =
      S3ClientProvider(
        config(endpoint = "http://localhost:29090", secretKey = null),
      ).provide().serviceClientConfiguration()

    assertThat(clientConfig.credentialsProvider()).isInstanceOf(DefaultCredentialsProvider::class.java)
  }

  private fun interceptorClassNames(endpoint: String?): List<String> =
    S3ClientProvider(config(endpoint = endpoint))
      .provide()
      .serviceClientConfiguration()
      .overrideConfiguration()
      .executionInterceptors()
      .map { it.javaClass.name }

  private fun config(
    endpoint: String?,
    accessKey: String? = "test-access-key",
    secretKey: String? = "test-secret-key",
  ) = ContentStorageS3Properties(
    bucketName = "test-bucket",
    accessKey = accessKey,
    secretKey = secretKey,
    endpoint = endpoint,
    signingRegion = "us-east-1",
  )
}
