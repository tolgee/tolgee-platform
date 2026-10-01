package io.tolgee.mcp

import io.tolgee.security.oauth2.OAuth2BearerChallengeProvider
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.module.kotlin.jacksonObjectMapper

class McpAuthChallengeFilterTest {
  private val challengeProvider: OAuth2BearerChallengeProvider =
    mock { on { challengeFor(any(), eq(HttpStatus.UNAUTHORIZED)) } doReturn CHALLENGE }

  private val filter = McpAuthChallengeFilter(challengeProvider, jacksonObjectMapper())

  @Test
  fun `a credential-less tools call is answered with 401 and the bearer challenge`() {
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()

    filter.doFilter(request(TOOLS_CALL), response, chain)

    response.status.assert.isEqualTo(401)
    response.getHeader("WWW-Authenticate").assert.isEqualTo(CHALLENGE)
    chain.request.assert
      .withFailMessage("the challenged request must not reach the transport")
      .isNull()
  }

  @Test
  fun `a provider with nothing to point at still produces a bare Bearer challenge`() {
    val provider: OAuth2BearerChallengeProvider = mock { on { challengeFor(any(), any()) } doReturn null }
    val response = MockHttpServletResponse()

    McpAuthChallengeFilter(provider, jacksonObjectMapper()).doFilter(request(TOOLS_CALL), response, MockFilterChain())

    response.status.assert.isEqualTo(401)
    response.getHeader("WWW-Authenticate").assert.isEqualTo("Bearer")
  }

  @Test
  fun `initialize passes through and the body is re-served on every stream access`() {
    val body = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"note":"přeložit"}}"""
    val chain = MockFilterChain()

    filter.doFilter(request(body), MockHttpServletResponse(), chain)

    val downstream = chain.request!!
    String(downstream.inputStream.readBytes(), Charsets.UTF_8).assert.isEqualTo(body)
    String(downstream.inputStream.readBytes(), Charsets.UTF_8).assert.isEqualTo(body)
    downstream.reader
      .readText()
      .assert
      .isEqualTo(body)
  }

  @Test
  fun `tools list and ping stay unchallenged`() {
    listOf("tools/list", "ping").forEach { method ->
      val chain = MockFilterChain()
      val body = """{"jsonrpc":"2.0","id":1,"method":"$method"}"""

      filter.doFilter(request(body), MockHttpServletResponse(), chain)

      chain.request.assert
        .withFailMessage("%s must stay public", method)
        .isNotNull()
    }
  }

  @Test
  fun `a request carrying any credential form is passed through untouched`() {
    val withAuthorization = request(TOOLS_CALL).apply { addHeader("Authorization", "Bearer nonsense") }
    val withApiKeyHeader = request(TOOLS_CALL).apply { addHeader("X-API-Key", "tgpak_nonsense") }
    val withAkParam = request(TOOLS_CALL, query = "ak=tgpak_nonsense")
    val withValuelessAk = request(TOOLS_CALL, query = "foo=1&ak")

    listOf(withAuthorization, withApiKeyHeader, withAkParam, withValuelessAk).forEach { credentialed ->
      val chain = MockFilterChain()

      filter.doFilter(credentialed, MockHttpServletResponse(), chain)

      chain.request.assert
        .withFailMessage("a credentialed request must pass through without wrapping")
        .isSameAs(credentialed)
    }
  }

  @Test
  fun `a query string that merely mentions ak is not a credential`() {
    listOf("trace=1", "akx=1", "lang=cs&bak=1", "q=ak", "x=1&yak=2").forEach { query ->
      val response = MockHttpServletResponse()
      val chain = MockFilterChain()

      filter.doFilter(request(TOOLS_CALL, query = query), response, chain)

      response.status.assert
        .withFailMessage("%s carries no credential, so it must still be challenged", query)
        .isEqualTo(401)
    }
  }

  @Test
  fun `a body this filter cannot classify is challenged rather than passed on`() {
    val oversized = """{"pad":"${"A".repeat(McpAuthChallengeFilter.PEEK_CAP)}","method":"initialize"}"""
    val cases =
      listOf(oversized, "[$TOOLS_CALL]", "this is not json", """{"method":{"nested":"initialize"}}""", "")

    cases.forEach { body ->
      val chain = MockFilterChain()
      val response = MockHttpServletResponse()

      filter.doFilter(request(body), response, chain)

      chain.request.assert
        .withFailMessage("body %s must not reach the transport", body)
        .isNull()
      response.status.assert
        .withFailMessage("body %s must be challenged", body)
        .isEqualTo(401)
    }
  }

  @Test
  fun `a method the open list does not name is challenged`() {
    listOf("resources/read", "prompts/get", "completion/complete").forEach { method ->
      val chain = MockFilterChain()
      val response = MockHttpServletResponse()

      filter.doFilter(request("""{"jsonrpc":"2.0","id":1,"method":"$method"}"""), response, chain)

      chain.request.assert
        .withFailMessage("%s must not reach the transport", method)
        .isNull()
      response.status.assert
        .withFailMessage("%s must be challenged", method)
        .isEqualTo(401)
    }
  }

  @Test
  fun `a notification is not challenged`() {
    val chain = MockFilterChain()

    filter.doFilter(
      request("""{"jsonrpc":"2.0","method":"notifications/initialized"}"""),
      MockHttpServletResponse(),
      chain,
    )

    chain.request.assert.isNotNull()
  }

  @Test
  fun `only POST is peeked, the SSE stream and session teardown are untouched`() {
    listOf("GET", "DELETE").forEach { method ->
      val chain = MockFilterChain()
      val nonPost = request(TOOLS_CALL, method = method)

      filter.doFilter(nonPost, MockHttpServletResponse(), chain)

      chain.request.assert.isSameAs(nonPost)
    }
  }

  private fun request(
    body: String,
    query: String? = null,
    method: String = "POST",
  ): MockHttpServletRequest =
    MockHttpServletRequest(method, McpConstants.DEVELOPER_ENDPOINT_PATH).apply {
      queryString = query
      contentType = "application/json"
      setContent(body.toByteArray(Charsets.UTF_8))
    }

  companion object {
    private const val CHALLENGE = """Bearer resource_metadata="https://tolgee.example.com/.well-known/x""""
    private const val TOOLS_CALL =
      """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"list_keys","arguments":{}}}"""
  }
}
