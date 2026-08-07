package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Component
class AppManifestHttpClient(
  @Qualifier("appsRestTemplate")
  private val restTemplate: RestTemplate,
) {
  fun fetchBody(url: String): String {
    val body =
      try {
        restTemplate.execute(url, HttpMethod.GET, null, { response ->
          if (!response.statusCode.is2xxSuccessful) {
            throw BadRequestException(
              Message.APP_MANIFEST_FETCH_FAILED,
              listOf("unexpected status ${response.statusCode.value()}"),
            )
          }
          readBounded(response.body)
        })
      } catch (e: RestClientException) {
        throw BadRequestException(Message.APP_MANIFEST_FETCH_FAILED, listOf(e.message ?: ""))
      }
    return body ?: throw BadRequestException(Message.APP_MANIFEST_FETCH_FAILED)
  }

  /**
   * The socket timeout only bounds the gap between two reads, so a host trickling bytes just under
   * it can hold a worker thread for as long as it likes. The wall-clock deadline is what actually
   * bounds that.
   */
  internal fun readBounded(
    stream: InputStream,
    maxReadTimeMs: Long = MAX_READ_TIME_MS,
  ): String {
    val deadline = System.currentTimeMillis() + maxReadTimeMs
    val buffer = ByteArray(READ_CHUNK_BYTES)
    val out = ByteArrayOutputStream()

    while (true) {
      if (System.currentTimeMillis() > deadline) {
        throw BadRequestException(
          Message.APP_MANIFEST_FETCH_FAILED,
          listOf("manifest not delivered within $maxReadTimeMs ms"),
        )
      }
      val read = stream.read(buffer)
      if (read == -1) break
      out.write(buffer, 0, read)
      if (out.size() > MAX_MANIFEST_SIZE_BYTES) {
        throw BadRequestException(
          Message.APP_MANIFEST_INVALID,
          listOf("manifest exceeds $MAX_MANIFEST_SIZE_BYTES bytes"),
        )
      }
    }
    return out.toString(Charsets.UTF_8)
  }

  companion object {
    const val MAX_MANIFEST_SIZE_BYTES = 256 * 1024
    private const val MAX_READ_TIME_MS = 10_000L
    private const val READ_CHUNK_BYTES = 8 * 1024
  }
}
