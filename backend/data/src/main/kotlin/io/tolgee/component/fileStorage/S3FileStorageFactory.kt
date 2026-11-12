package io.tolgee.component.fileStorage

import io.tolgee.model.contentDelivery.S3Config
import io.tolgee.util.SsrfSafeDnsResolver
import io.tolgee.util.SsrfSafeRequestFactoryProvider
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.routing.HttpRoutePlanner
import org.springframework.stereotype.Component

@Component
class S3FileStorageFactory(
  private val ssrfSafeDnsResolver: SsrfSafeDnsResolver,
) {
  /** The server's own file storage (operator configuration, same trust level as any other tolgee.* property). */
  fun create(config: S3Config): S3FileStorage = build(config, dnsResolver = null, routePlanner = null)

  /**
   * A content-delivery storage, whose endpoint is user-influenced, so its client pins DNS to the addresses
   * [UrlSecurity][io.tolgee.util.UrlSecurity] vets. @param allowLocalAddresses whether a local/internal endpoint is
   * permitted (true for the operator's server-level default storage, the project flag otherwise).
   */
  fun create(
    config: S3Config,
    allowLocalAddresses: Boolean,
  ): S3FileStorage =
    build(
      config,
      // Exempt the operator's egress-proxy hop from the address rules, and refuse a direct connection to the proxy
      // host, reusing the exact logic SsrfSafeRequestFactoryProvider applies to webhooks/SSO/LLM calls.
      ssrfSafeDnsResolver.create(allowLocalAddresses) { SsrfSafeRequestFactoryProvider.isOperatorProxy(it) },
      SsrfSafeRequestFactoryProvider.proxyHopOnlyRoutePlanner(),
    )

  private fun build(
    config: S3Config,
    dnsResolver: DnsResolver?,
    routePlanner: HttpRoutePlanner?,
  ): S3FileStorage {
    val client = S3ClientProvider(config, dnsResolver, routePlanner).provide()
    val bucketName = config.bucketName ?: throw RuntimeException("Bucket name for S3 storage is not set")
    return S3FileStorage(bucketName = bucketName, path = config.path, s3 = client, publicRead = config.publicRead)
  }
}
