package io.tolgee.component.fileStorage

import io.tolgee.model.contentDelivery.S3Config
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider
import org.apache.hc.client5.http.routing.HttpRoutePlanner
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.http.apache5.Apache5HttpClient
import software.amazon.awssdk.http.apache5.ProxyConfiguration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.LegacyMd5Plugin
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

class S3ClientProvider internal constructor(
  private val s3config: S3Config,
  // When set, pins the client's DNS so it connects only to pre-validated addresses. Null keeps the SDK default
  // resolver. Kept internal so the Apache HttpClient 5 types stay inside this module; callers outside use the
  // single-argument constructor and the S3FileStorageFactory overloads to opt into pinning.
  private val dnsResolver: DnsResolver?,
  // When set, routes through the operator's egress proxy (if any) while refusing a direct connection to the proxy
  // host, so the dnsResolver's proxy-hop exemption cannot be abused. Pairs with dnsResolver on the pinned path.
  private val routePlanner: HttpRoutePlanner?,
) {
  constructor(s3config: S3Config) : this(s3config, null, null)

  fun provide(): S3Client {
    val credentialsProvider = getCredentialsProvider()

    val builder =
      S3Client.builder().credentialsProvider(credentialsProvider).serviceConfiguration(
        S3Configuration.builder().pathStyleAccessEnabled(true).build(),
      )
    if (dnsResolver != null || routePlanner != null) {
      val httpClientBuilder = Apache5HttpClient.builder()
      dnsResolver?.let { httpClientBuilder.dnsResolver(it) }
      routePlanner?.let {
        // Make the route planner the single proxy authority: it reads the JVM proxy properties itself and refuses a
        // direct connection to the proxy host. The SDK rejects httpRoutePlanner together with an enabled
        // proxyConfiguration, and its default proxyConfiguration is enabled whenever http(s).proxyHost is set, so turn
        // the SDK's own proxy resolution off here. Restore proxy authentication (http.proxyUser/http.proxyPassword)
        // that the disabled proxyConfiguration would otherwise have supplied, via the same system-default credentials
        // provider SsrfSafeRequestFactoryProvider gets from useSystemProperties(). Mirrors SsrfSafeRequestFactoryProvider.
        httpClientBuilder
          .httpRoutePlanner(it)
          .proxyConfiguration(
            ProxyConfiguration
              .builder()
              .useSystemPropertyValues(false)
              .useEnvironmentVariableValues(false)
              .build(),
          ).credentialsProvider(SystemDefaultCredentialsProvider())
      }
      builder.httpClientBuilder(httpClientBuilder)
    }
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
      true -> DefaultCredentialsProvider.builder().build()
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
