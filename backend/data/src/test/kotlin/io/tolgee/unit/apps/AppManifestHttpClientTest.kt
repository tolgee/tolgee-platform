package io.tolgee.unit.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.io.InputStream

class AppManifestHttpClientTest {
  private val restTemplate = RestTemplate()
  private val server: MockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
  private val client = AppManifestHttpClient(restTemplate)

  private val url = "https://app.example.com/manifest.json"

  @Test
  fun `returns the body of a successful response`() {
    server
      .expect(requestTo(url))
      .andRespond(withSuccess("""{"id":"a"}""", MediaType.APPLICATION_JSON))

    client.fetchBody(url).assert.isEqualTo("""{"id":"a"}""")
  }

  @Test
  fun `rejects a manifest larger than the size limit`() {
    val tooBig = "x".repeat(AppManifestHttpClient.MAX_MANIFEST_SIZE_BYTES + 1)
    server
      .expect(requestTo(url))
      .andRespond(withSuccess(tooBig, MediaType.APPLICATION_JSON))

    val exception = assertThrows<BadRequestException> { client.fetchBody(url) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_INVALID.code)
  }

  @Test
  fun `rejects a body that is not delivered within the read deadline`() {
    val trickling =
      object : InputStream() {
        override fun read(
          b: ByteArray,
          off: Int,
          len: Int,
        ): Int {
          Thread.sleep(20)
          b[off] = 'x'.code.toByte()
          return 1
        }

        override fun read(): Int = 'x'.code
      }

    val exception =
      assertThrows<BadRequestException> {
        client.readBounded(trickling, maxReadTimeMs = 100)
      }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_FETCH_FAILED.code)
  }

  @Test
  fun `rejects a redirect response rather than following it`() {
    server
      .expect(requestTo(url))
      .andRespond(withStatus(HttpStatus.FOUND).header("Location", "http://169.254.169.254/"))

    val exception = assertThrows<BadRequestException> { client.fetchBody(url) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_FETCH_FAILED.code)
  }
}
