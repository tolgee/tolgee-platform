package io.tolgee.mcp

import io.tolgee.security.oauth2.OAuth2BearerChallengeProvider
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.ObjectMapper

class McpAuthChallengeFilterTest {
  private val challenge = "Bearer resource_metadata=\"https://t/.well-known/oauth-protected-resource/mcp\""

  private val challengeProvider =
    mock<OAuth2BearerChallengeProvider> {
      on { challengeFor(any(), any()) } doReturn challenge
    }

  private val filter = McpAuthChallengeFilter(challengeProvider, ObjectMapper())

  private fun toolsCallRequest(): MockHttpServletRequest =
    postRequest("""{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"list_keys"}}""")

  private fun postRequest(body: String): MockHttpServletRequest {
    val request = MockHttpServletRequest("POST", McpConstants.DEVELOPER_ENDPOINT_PATH)
    request.setContent(body.toByteArray(Charsets.UTF_8))
    return request
  }

  @Test
  fun `a tools-call post without credentials is answered 401 with the challenge`() {
    val request = toolsCallRequest()
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()

    filter.doFilter(request, response, chain)

    response.status.assert.isEqualTo(401)
    response.getHeader(HttpHeaders.WWW_AUTHENTICATE).assert.isEqualTo(challenge)
    chain.request.assert.isNull()
  }

  @Test
  fun `a tools-call with a bearer header passes through with a re-readable body`() {
    val request = toolsCallRequest()
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer x")
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()

    filter.doFilter(request, response, chain)

    chain.request.assert.isNotNull()
    val forwarded = chain.request as jakarta.servlet.http.HttpServletRequest
    val body = forwarded.inputStream.readAllBytes().toString(Charsets.UTF_8)
    body.assert.contains("tools/call")
  }

  @Test
  fun `a tools-call with an api key header passes through`() {
    val request = toolsCallRequest()
    request.addHeader("X-API-Key", "tgpak_x")
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()

    filter.doFilter(request, response, chain)

    chain.request.assert.isNotNull()
    response.status.assert.isEqualTo(200)
  }

  @Test
  fun `a tools-call with an ak query parameter passes through unchallenged`() {
    // AuthenticationFilter also accepts request.getParameter("ak") as a credential; this filter must not challenge
    // a request that would otherwise authenticate downstream. Set via queryString directly, not addParameter —
    // getParameter would consume the request here before the real filter/servlet ever peeks the body.
    val request = toolsCallRequest()
    request.queryString = "ak=tgpak_x"
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()

    filter.doFilter(request, response, chain)

    chain.request.assert.isNotNull()
    response.status.assert.isEqualTo(200)
  }

  @Test
  fun `initialize and tools-list without credentials pass through`() {
    val initializeRequest = postRequest("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}""")
    val initializeResponse = MockHttpServletResponse()
    val initializeChain = MockFilterChain()

    filter.doFilter(initializeRequest, initializeResponse, initializeChain)

    initializeChain.request.assert.isNotNull()
    initializeResponse.status.assert.isEqualTo(200)

    val listRequest = postRequest("""{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""")
    val listResponse = MockHttpServletResponse()
    val listChain = MockFilterChain()

    filter.doFilter(listRequest, listResponse, listChain)

    listChain.request.assert.isNotNull()
    listResponse.status.assert.isEqualTo(200)
  }

  @Test
  fun `a get request passes through untouched`() {
    val request = MockHttpServletRequest("GET", McpConstants.DEVELOPER_ENDPOINT_PATH)
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()

    filter.doFilter(request, response, chain)

    (chain.request === request).assert.isTrue()
  }

  @Test
  fun `an unparseable body passes through`() {
    val request = postRequest("not valid json")
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()

    filter.doFilter(request, response, chain)

    chain.request.assert.isNotNull()
    response.status.assert.isEqualTo(200)
  }

  @Test
  fun `a tools-call body over the peek cap passes through unchallenged and stays fully readable`() {
    val hugeBody =
      """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"pad":"${"a".repeat(70 * 1024)}"}}"""
    val request = postRequest(hugeBody)
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()

    filter.doFilter(request, response, chain)

    response.status.assert.isEqualTo(200)
    chain.request.assert.isNotNull()
    val forwarded = chain.request as jakarta.servlet.http.HttpServletRequest
    val body = forwarded.inputStream.readAllBytes().toString(Charsets.UTF_8)
    body.assert.isEqualTo(hugeBody)
  }

  @Test
  fun `a credential-less non-tools-call request stays readable through getReader too`() {
    val body = """{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}"""
    val request = postRequest(body)
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()

    filter.doFilter(request, response, chain)

    val forwarded = chain.request as jakarta.servlet.http.HttpServletRequest
    forwarded.reader
      .readText()
      .assert
      .isEqualTo(body)
  }
}
