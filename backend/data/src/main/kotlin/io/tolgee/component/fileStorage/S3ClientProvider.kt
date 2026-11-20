package io.tolgee.component.fileStorage

import io.tolgee.model.contentDelivery.S3Config
import io.tolgee.util.SsrfSafeRequestFactoryProvider
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider
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
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

class S3ClientProvider(
  private val s3config: S3Config,
  private val dnsResolver: DnsResolver? = null,
) {
  fun provide(): S3Client {
    val credentialsProvider = getCredentialsProvider()

    val builder =
      S3Client.builder().credentialsProvider(credentialsProvider).serviceConfiguration(
        S3Configuration.builder().pathStyleAccessEnabled(true).build(),
      )
    dnsResolver?.let { builder.httpClientBuilder(pinnedHttpClient(it)) }
    if (!s3config.endpoint.isNullOrEmpty()) {
      builder.configureForCustomEndpoint()
    }
    if (!s3config.signingRegion.isNullOrEmpty()) {
      builder.region(Region.of(s3config.signingRegion))
    }
    return builder.build()
  }

  // The SDK refuses httpRoutePlanner while its own proxyConfiguration is enabled, which it is by default whenever
  // http(s).proxyHost is set. Disabling it also drops proxy credentials, hence the system-default credentials provider.
  private fun pinnedHttpClient(dnsResolver: DnsResolver): Apache5HttpClient.Builder =
    Apache5HttpClient
      .builder()
      .dnsResolver(dnsResolver)
      .httpRoutePlanner(SsrfSafeRequestFactoryProvider.proxyHopOnlyRoutePlanner(HttpProxyForHttpsSelector))
      .proxyConfiguration(
        ProxyConfiguration
          .builder()
          .useSystemPropertyValues(false)
          .useEnvironmentVariableValues(false)
          .build(),
      ).credentialsProvider(SystemDefaultCredentialsProvider())

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

private object HttpProxyForHttpsSelector : ProxySelector() {
  override fun select(uri: URI): MutableList<Proxy> {
    val default = getDefault()
    val selected = default.select(uri)
    if (!"https".equals(uri.scheme, ignoreCase = true) || selected.any { it.type() != Proxy.Type.DIRECT }) {
      return selected
    }
    val asHttp =
      runCatching { URI("http", uri.userInfo, uri.host, uri.port, uri.path, uri.query, uri.fragment) }
        .getOrNull() ?: return selected
    return default.select(asHttp)
  }

  override fun connectFailed(
    uri: URI,
    sa: SocketAddress,
    e: IOException,
  ) = getDefault().connectFailed(uri, sa, e)
}
