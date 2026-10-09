package io.tolgee.mcp

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class PeekedRequestBodyTest {
  @Test
  fun `the replayed request serves the body again on every stream and reader access`() {
    val body = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"note":"přeložit"}}"""

    val replayed = PeekedRequestBody.peek(request(body), cap = 1024).replay()

    String(replayed.inputStream.readBytes(), Charsets.UTF_8).assert.isEqualTo(body)
    String(replayed.inputStream.readBytes(), Charsets.UTF_8).assert.isEqualTo(body)
    replayed.reader
      .readText()
      .assert
      .isEqualTo(body)
  }

  @Test
  fun `a body exactly at the cap is complete and not over it`() {
    val body = "A".repeat(CAP)

    val peeked = PeekedRequestBody.peek(request(body), CAP)

    peeked.overCap.assert.isFalse()
    String(peeked.bytes, Charsets.UTF_8).assert.isEqualTo(body)
  }

  @Test
  fun `a body one byte past the cap is reported as over it`() {
    val peeked = PeekedRequestBody.peek(request("A".repeat(CAP + 1)), CAP)

    peeked.overCap.assert.isTrue()
    peeked.bytes.size.assert
      .isEqualTo(CAP)
  }

  @Test
  fun `an empty body peeks as empty`() {
    val peeked = PeekedRequestBody.peek(request(""), CAP)

    peeked.overCap.assert.isFalse()
    peeked.bytes.size.assert
      .isEqualTo(0)
  }

  private fun request(body: String): MockHttpServletRequest =
    MockHttpServletRequest("POST", McpConstants.DEVELOPER_ENDPOINT_PATH).apply {
      contentType = "application/json"
      setContent(body.toByteArray(Charsets.UTF_8))
    }

  companion object {
    private const val CAP = 32
  }
}
