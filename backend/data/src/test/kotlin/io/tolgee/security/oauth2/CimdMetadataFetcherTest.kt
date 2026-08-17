/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.oauth2

import com.sun.net.httpserver.HttpServer
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.util.UrlSecurity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Focused on the security gates of CIMD resolution: a client-id URL that is non-https, internal (SSRF), off the
 * allow-list, or arrives while CIMD is disabled/unconfigured must never be fetched or resolved to a client; and a
 * fetched document must pass the client_id / auth-method / grant / same-origin-redirect gates in [buildClient].
 */
class CimdMetadataFetcherTest {
  private val mapper = jacksonObjectMapper()

  private fun fetcher(
    ssrfDisabled: Boolean,
    properties: OAuth2CimdProperties = OAuth2CimdProperties().apply { allowedHosts = listOf("example.com") },
  ): CimdMetadataFetcher {
    val internalProperties = InternalProperties().apply { disableUrlSsrfProtection = ssrfDisabled }
    return CimdMetadataFetcher(
      properties,
      OAuth2ServerProperties(),
      UrlSecurity(internalProperties),
      mapper,
    )
  }

  private fun validDocument(
    clientId: String = "https://example.com/client",
    authMethod: String = "none",
    grantTypes: List<String> = listOf("authorization_code"),
    redirectUris: List<String> = listOf("https://example.com/callback"),
  ): JsonNode {
    val node = mapper.createObjectNode()
    node.put("client_id", clientId)
    node.put("token_endpoint_auth_method", authMethod)
    node.set("grant_types", mapper.valueToTree<JsonNode>(grantTypes))
    node.set("redirect_uris", mapper.valueToTree<JsonNode>(redirectUris))
    return node
  }

  @Test
  fun `rejects a non-https client id`() {
    val properties = OAuth2CimdProperties().apply { allowedHosts = listOf("example.com") }
    assertThat(fetcher(ssrfDisabled = true, properties).fetchAndValidate("http://example.com/client")).isNull()
  }

  @Test
  fun `rejects a loopback client id under the SSRF guard`() {
    val properties = OAuth2CimdProperties().apply { allowedHosts = listOf("127.0.0.1") }
    assertThat(fetcher(ssrfDisabled = false, properties).fetchAndValidate("https://127.0.0.1/client")).isNull()
  }

  @Test
  fun `rejects a host outside the allow-list`() {
    val properties = OAuth2CimdProperties().apply { allowedHosts = listOf("trusted.example.com") }
    assertThat(fetcher(ssrfDisabled = true, properties).fetchAndValidate("https://evil.example.com/client")).isNull()
  }

  @Test
  fun `resolves nothing when CIMD is disabled`() {
    val properties = OAuth2CimdProperties().apply { enabled = false }
    assertThat(fetcher(ssrfDisabled = true, properties).fetchAndValidate("https://example.com/client")).isNull()
  }

  @Test
  fun `resolves nothing when enabled but no allow-list is configured (fail-closed)`() {
    val properties = OAuth2CimdProperties().apply { allowedHosts = listOf() }
    assertThat(fetcher(ssrfDisabled = true, properties).fetchAndValidate("https://example.com/client")).isNull()
  }

  @Test
  fun `buildClient accepts a well-formed document`() {
    val client = fetcher(ssrfDisabled = true).buildClient("https://example.com/client", validDocument())
    assertThat(client).isNotNull
    assertThat(client!!.clientId).isEqualTo("https://example.com/client")
    assertThat(client.clientSettings.isRequireProofKey).isTrue()
    assertThat(client.clientSettings.isRequireAuthorizationConsent).isTrue()
  }

  @Test
  fun `buildClient rejects a document whose client_id does not equal the requested url`() {
    val document = validDocument(clientId = "https://example.com/other")
    assertThat(fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)).isNull()
  }

  @Test
  fun `buildClient rejects a non-textual client_id instead of throwing`() {
    // A malformed document can give client_id as an object/array; Jackson's asString() throws on those, so buildClient
    // must read it fail-closed and return null (buildClient runs outside fetch's catch, so a throw would escape).
    val document = mapper.createObjectNode()
    document.set("client_id", mapper.createObjectNode().put("url", "https://example.com/client"))
    document.put("token_endpoint_auth_method", "none")
    document.set("grant_types", mapper.valueToTree<JsonNode>(listOf("authorization_code")))
    document.set("redirect_uris", mapper.valueToTree<JsonNode>(listOf("https://example.com/callback")))
    assertThat(fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)).isNull()
  }

  @Test
  fun `buildClient rejects a non-public token_endpoint_auth_method`() {
    val document = validDocument(authMethod = "client_secret_basic")
    assertThat(fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)).isNull()
  }

  @Test
  fun `buildClient rejects grant_types that lack authorization_code`() {
    val document = validDocument(grantTypes = listOf("refresh_token"))
    assertThat(fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)).isNull()
  }

  @Test
  fun `buildClient rejects an empty redirect_uris`() {
    val document = validDocument(redirectUris = listOf())
    assertThat(fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)).isNull()
  }

  @Test
  fun `buildClient rejects a redirect_uri on a foreign origin (open-redirect hijack)`() {
    val document = validDocument(redirectUris = listOf("https://evil.example.com/callback"))
    assertThat(fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)).isNull()
  }

  @Test
  fun `buildClient rejects a same-host redirect_uri on a different explicit port`() {
    val document = validDocument(redirectUris = listOf("https://example.com:8443/callback"))
    assertThat(fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)).isNull()
  }

  @Test
  fun `buildClient accepts a same-host redirect_uri that states the default https port explicitly`() {
    val document = validDocument(redirectUris = listOf("https://example.com:443/callback"))
    assertThat(fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)).isNotNull()
  }

  @Test
  fun `buildClient rejects a same-host redirect_uri on a different scheme`() {
    val document = validDocument(redirectUris = listOf("http://example.com/callback"))
    assertThat(fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)).isNull()
  }

  @Test
  fun `readCapped returns the body at exactly the size cap`() {
    val properties =
      OAuth2CimdProperties().apply {
        maxDocumentBytes = 16
        allowedHosts = listOf("example.com")
      }
    val body = ByteArray(16) { 'a'.code.toByte() }
    assertThat(fetcher(ssrfDisabled = true, properties).readCapped(body.inputStream())).hasSize(16)
  }

  @Test
  fun `readCapped rejects a body one byte over the cap`() {
    val properties =
      OAuth2CimdProperties().apply {
        maxDocumentBytes = 16
        allowedHosts = listOf("example.com")
      }
    val body = ByteArray(17) { 'a'.code.toByte() }
    assertThat(fetcher(ssrfDisabled = true, properties).readCapped(body.inputStream())).isNull()
  }

  @Test
  fun `buildClient grants a refresh token only when the document does not opt out`() {
    val withRefresh =
      fetcher(ssrfDisabled = true)
        .buildClient(
          "https://example.com/client",
          validDocument(grantTypes = listOf("authorization_code", "refresh_token")),
        )
    assertThat(withRefresh!!.authorizationGrantTypes.map { it.value }).contains("refresh_token")

    val withoutRefresh =
      fetcher(ssrfDisabled = true)
        .buildClient("https://example.com/client", validDocument(grantTypes = listOf("authorization_code")))
    assertThat(withoutRefresh!!.authorizationGrantTypes.map { it.value }).doesNotContain("refresh_token")
  }

  @Test
  fun `buildClient id changes when redirect_uris change (forces re-consent)`() {
    val first = fetcher(ssrfDisabled = true).buildClient("https://example.com/client", validDocument())
    val second =
      fetcher(ssrfDisabled = true).buildClient(
        "https://example.com/client",
        validDocument(redirectUris = listOf("https://example.com/callback", "https://example.com/callback2")),
      )
    assertThat(first!!.id).isNotEqualTo(second!!.id)
  }

  @Test
  fun `fetch returns the document on 200, null on non-200, and does not follow redirects`() {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val redirectTargetHit = AtomicBoolean(false)
    server.createContext("/ok") { ex ->
      val body = mapper.writeValueAsBytes(validDocument())
      ex.sendResponseHeaders(200, body.size.toLong())
      ex.responseBody.use { it.write(body) }
    }
    server.createContext("/notfound") { ex ->
      ex.sendResponseHeaders(404, -1)
      ex.close()
    }
    server.createContext("/redirect") { ex ->
      ex.responseHeaders.add("Location", "/target")
      ex.sendResponseHeaders(302, -1)
      ex.close()
    }
    server.createContext("/target") { ex ->
      redirectTargetHit.set(true)
      ex.sendResponseHeaders(200, -1)
      ex.close()
    }
    server.start()
    try {
      val base = "http://127.0.0.1:${server.address.port}"
      val fetcher = fetcher(ssrfDisabled = true)
      assertThat(fetcher.fetch("$base/ok")).isNotNull
      assertThat(fetcher.fetch("$base/notfound")).isNull()
      // The client is built with Redirect.NEVER (SSRF: an allow-listed host must not 302 us onto an internal address).
      assertThat(fetcher.fetch("$base/redirect")).isNull()
      assertThat(redirectTargetHit.get()).isFalse()
    } finally {
      server.stop(0)
    }
  }
}
