package io.tolgee.ee.api.v2.controllers

import io.tolgee.AbstractSpringTest
import io.tolgee.component.contentDelivery.ContentDeliveryUploader
import io.tolgee.development.testDataBuilder.data.ContentStoragePublishTestData
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ContentStoragePublishSsrfTest : AbstractSpringTest() {
  @Autowired
  private lateinit var contentDeliveryUploader: ContentDeliveryUploader

  private lateinit var testData: ContentStoragePublishTestData

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
    val connected =
      connectionArrives(timeoutSeconds = 1) { port ->
        val configId = saveConfigWithEndpoint("http://127.0.0.1:$port")
        assertThrows<Exception> { contentDeliveryUploader.upload(configId) }
      }

    connected.assert.isFalse()
  }

  @Test
  fun `connects to a loopback storage endpoint when local addresses are allowed`() {
    tolgeeProperties.contentDelivery.storage.allowLocalAddresses = true

    val connected =
      connectionArrives { port ->
        val configId = saveConfigWithEndpoint("http://127.0.0.1:$port")
        runCatching { contentDeliveryUploader.upload(configId) }
      }

    connected.assert.isTrue()
  }

  @Test
  fun `routes a publish through the operator proxy instead of refusing the private proxy host`() {
    val reachedProxy =
      connectionArrives { proxyPort ->
        withSystemProperties("http.proxyHost" to "127.0.0.1", "http.proxyPort" to "$proxyPort") {
          val configId = saveConfigWithEndpoint("http://93.184.216.34/")
          runCatching { contentDeliveryUploader.upload(configId) }
        }
      }

    reachedProxy.assert.isTrue()
  }

  @Test
  fun `routes an https publish through an http-only operator proxy`() {
    val reachedProxy =
      connectionArrives { proxyPort ->
        withSystemProperties("http.proxyHost" to "127.0.0.1", "http.proxyPort" to "$proxyPort") {
          val configId = saveConfigWithEndpoint("https://93.184.216.34/")
          runCatching { contentDeliveryUploader.upload(configId) }
        }
      }

    reachedProxy.assert.isTrue()
  }

  @Test
  fun `answers the operator proxy's authentication challenge with the JVM proxy credentials`() {
    ServerSocket(0, 50, LOOPBACK).use { proxy ->
      val authorization = CompletableFuture<String>()
      challengeForProxyAuthentication(proxy) { authorization.complete(it) }

      withSystemProperties(
        "http.proxyHost" to "127.0.0.1",
        "http.proxyPort" to "${proxy.localPort}",
        "http.proxyUser" to "proxy-user",
        "http.proxyPassword" to "proxy-secret",
      ) {
        val configId = saveConfigWithEndpoint("http://93.184.216.34/")
        runCatching { contentDeliveryUploader.upload(configId) }
      }

      val expected = "Basic " + Base64.getEncoder().encodeToString("proxy-user:proxy-secret".toByteArray())
      authorization.get(10, TimeUnit.SECONDS).assert.isEqualTo(expected)
    }
  }

  private fun saveConfigWithEndpoint(endpoint: String): Long {
    testData = ContentStoragePublishTestData(endpoint)
    testDataService.saveTestData(testData.root)
    return testData.contentDeliveryConfig.self.id
  }

  private fun withSystemProperties(
    vararg properties: Pair<String, String>,
    block: () -> Unit,
  ) {
    properties.forEach { (name, value) -> System.setProperty(name, value) }
    try {
      block()
    } finally {
      properties.forEach { (name, _) -> System.clearProperty(name) }
    }
  }

  private fun connectionArrives(
    timeoutSeconds: Long = 10,
    block: (port: Int) -> Unit,
  ): Boolean {
    ServerSocket(0, 1, LOOPBACK).use { serverSocket ->
      val connected = CompletableFuture<Unit>()
      onDaemonThread { serverSocket.accept().use { connected.complete(Unit) } }
      block(serverSocket.localPort)
      return runCatching { connected.get(timeoutSeconds, TimeUnit.SECONDS) }.isSuccess
    }
  }

  private fun challengeForProxyAuthentication(
    proxy: ServerSocket,
    onAuthorization: (String) -> Unit,
  ) {
    onDaemonThread {
      while (true) {
        proxy.accept().use { connection ->
          val headers =
            connection
              .getInputStream()
              .bufferedReader()
              .lineSequence()
              .takeWhile { it.isNotEmpty() }
              .toList()
          headers
            .firstOrNull { it.startsWith("Proxy-Authorization:", ignoreCase = true) }
            ?.let { onAuthorization(it.substringAfter(":").trim()) }
          connection.getOutputStream().write(PROXY_AUTH_CHALLENGE.toByteArray())
        }
      }
    }
  }

  private fun onDaemonThread(block: () -> Unit) {
    Thread { runCatching(block) }.apply { isDaemon = true }.start()
  }

  companion object {
    private val LOOPBACK = InetAddress.getByName("127.0.0.1")

    private const val PROXY_AUTH_CHALLENGE =
      "HTTP/1.1 407 Proxy Authentication Required\r\n" +
        "Proxy-Authenticate: Basic realm=\"proxy\"\r\n" +
        "Content-Length: 0\r\n" +
        "Connection: close\r\n\r\n"
  }
}
