package io.tolgee.unit.component.fileStorage

import com.sun.net.httpserver.HttpServer
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.configuration.tolgee.ContentStorageS3Properties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.util.SsrfSafeDnsResolver
import io.tolgee.util.UrlSecurity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

class S3FileStorageFactoryTest {
  private lateinit var server: HttpServer
  private val aclHeaders = mutableListOf<String?>()

  @BeforeEach
  fun startServer() {
    server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
    server.createContext("/") { exchange ->
      aclHeaders.add(exchange.requestHeaders.getFirst("x-amz-acl"))
      exchange.requestBody.readAllBytes()
      exchange.sendResponseHeaders(200, -1)
      exchange.close()
    }
    server.start()
  }

  @AfterEach
  fun stopServer() {
    server.stop(0)
  }

  @Test
  fun `server-configured storage uploads as public-read when publicRead is set`() {
    store(publicRead = true)
    assertThat(aclHeaders).containsExactly("public-read")
  }

  @Test
  fun `server-configured storage uploads without ACL by default`() {
    store(publicRead = false)
    assertThat(aclHeaders).containsExactly(null)
  }

  private fun store(publicRead: Boolean) {
    val properties =
      ContentStorageS3Properties(
        bucketName = "bucket",
        accessKey = "access-key",
        secretKey = "secret-key",
        endpoint = "http://localhost:${server.address.port}",
        signingRegion = "us-east-1",
        publicRead = publicRead,
      )
    val internalProperties = InternalProperties()
    val factory = S3FileStorageFactory(SsrfSafeDnsResolver(UrlSecurity(internalProperties), internalProperties))
    factory.create(properties).storeFile("en.json", "{}".toByteArray())
  }
}
