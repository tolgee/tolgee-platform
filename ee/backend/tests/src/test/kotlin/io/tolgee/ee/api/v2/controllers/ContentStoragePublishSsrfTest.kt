package io.tolgee.ee.api.v2.controllers

import io.tolgee.AbstractSpringTest
import io.tolgee.component.contentDelivery.ContentDeliveryUploader
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.contentDelivery.S3ContentStorageConfig
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The GHSA-hqr8-67r5-xj69 fix validates a project storage endpoint only on create/update/test. A storage saved before
 * that fix (or one whose host re-resolves to an internal address after validation) still reaches the endpoint on every
 * publish. These tests drive the publish path directly and assert the storage client no longer connects to a loopback
 * address unless local addresses are explicitly allowed.
 */
class ContentStoragePublishSsrfTest : AbstractSpringTest() {
  @Autowired
  private lateinit var contentDeliveryUploader: ContentDeliveryUploader

  private lateinit var testData: PublishSsrfTestData

  @BeforeEach
  fun setup() {
    internalProperties.disableUrlSsrfProtection = false
    tolgeeProperties.contentDelivery.storage.allowLocalAddresses = false
  }

  @AfterEach
  fun cleanup() {
    if (::testData.isInitialized) {
      testDataService.cleanTestData(testData.root)
    }
    internalProperties.disableUrlSsrfProtection = true
    tolgeeProperties.contentDelivery.storage.allowLocalAddresses = false
  }

  @Test
  fun `does not connect to a loopback storage endpoint on publish`() {
    ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { socket ->
      val connected = CountDownLatch(1)
      acceptOnce(socket) { connected.countDown() }
      val configId = saveConfigWithEndpoint("http://127.0.0.1:${socket.localPort}")

      assertThrows<Exception> { contentDeliveryUploader.upload(configId) }

      connected.await(1, TimeUnit.SECONDS).assert.isFalse()
    }
  }

  @Test
  fun `connects to a loopback storage endpoint when local addresses are allowed`() {
    tolgeeProperties.contentDelivery.storage.allowLocalAddresses = true
    ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { socket ->
      val connected = CountDownLatch(1)
      acceptOnce(socket) { connected.countDown() }
      val configId = saveConfigWithEndpoint("http://127.0.0.1:${socket.localPort}")

      // Not a real S3 endpoint, so the upload still fails — but the client was allowed to reach the loopback address.
      runCatching { contentDeliveryUploader.upload(configId) }

      connected.await(10, TimeUnit.SECONDS).assert.isTrue()
    }
  }

  @Test
  fun `routes a publish through the operator proxy instead of refusing the private proxy host`() {
    ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { proxy ->
      val reachedProxy = CountDownLatch(1)
      acceptOnce(proxy) { reachedProxy.countDown() }
      withProxyProperties("127.0.0.1", proxy.localPort) {
        // With an http proxy configured the client connects to the proxy, so the resolver is handed the private
        // proxy host for a public endpoint; the proxy hop must be exempted, not blocked.
        val configId = saveConfigWithEndpoint("http://93.184.216.34/")

        runCatching { contentDeliveryUploader.upload(configId) }

        reachedProxy.await(10, TimeUnit.SECONDS).assert.isTrue()
      }
    }
  }

  @Test
  fun `routes an https publish through an http-only operator proxy`() {
    ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { proxy ->
      val reachedProxy = CountDownLatch(1)
      acceptOnce(proxy) { reachedProxy.countDown() }
      // Only http.proxyHost/http.proxyPort are set, yet the endpoint is https: the client must still route through
      // the proxy (an http proxy tunnels https via CONNECT), as the SDK's own proxy selection would.
      withProxyProperties("127.0.0.1", proxy.localPort) {
        val configId = saveConfigWithEndpoint("https://93.184.216.34/")

        runCatching { contentDeliveryUploader.upload(configId) }

        reachedProxy.await(10, TimeUnit.SECONDS).assert.isTrue()
      }
    }
  }

  private fun saveConfigWithEndpoint(endpoint: String): Long {
    testData = PublishSsrfTestData(endpoint)
    testDataService.saveTestData(testData.root)
    return testData.contentDeliveryConfig.self.id
  }

  private fun <T> withProxyProperties(
    host: String,
    port: Int,
    block: () -> T,
  ): T {
    System.setProperty("http.proxyHost", host)
    System.setProperty("http.proxyPort", port.toString())
    return try {
      block()
    } finally {
      System.clearProperty("http.proxyHost")
      System.clearProperty("http.proxyPort")
    }
  }

  /** Accepts a single connection on a daemon thread and signals it; stays quietly blocked if none ever arrives. */
  private fun acceptOnce(
    serverSocket: ServerSocket,
    onConnect: () -> Unit,
  ) {
    Thread {
      try {
        serverSocket.accept().use { onConnect() }
      } catch (_: Exception) {
        // The socket was closed while nothing had connected.
      }
    }.apply { isDaemon = true }.start()
  }
}

private class PublishSsrfTestData(
  s3Endpoint: String,
) : BaseTestData() {
  val s3ContentStorage =
    projectBuilder.addContentStorage {
      name = "Pre-existing S3"
      this.s3ContentStorageConfig =
        S3ContentStorageConfig(this).apply {
          bucketName = "bucket"
          accessKey = "accessKey"
          secretKey = "secretKey"
          endpoint = s3Endpoint
          signingRegion = "eu-central-1"
        }
    }

  val contentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      contentStorage = s3ContentStorage.self
      name = "Publishes to a pre-existing storage"
    }

  val key =
    projectBuilder.addKey("key") {
      addTranslation("en", "Hello")
    }
}
