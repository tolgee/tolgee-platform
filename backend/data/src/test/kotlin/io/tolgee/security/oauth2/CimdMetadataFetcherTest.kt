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
import io.tolgee.configuration.tolgee.OAuth2CimdProperties
import io.tolgee.testing.assert
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Focused on the security gates of CIMD resolution: a client-id URL that is non-https, internal (SSRF), or off a
 * configured allow-list must never be fetched or resolved to a client; and a fetched document must pass the
 * client_id / auth-method / grant / redirect gates in [CimdMetadataFetcher.buildClient].
 */
class CimdMetadataFetcherTest {
  private val mapper = jacksonObjectMapper()

  private fun fetcher(
    ssrfDisabled: Boolean,
    properties: OAuth2CimdProperties = OAuth2CimdProperties(),
  ): CimdMetadataFetcher {
    val internalProperties = InternalProperties().apply { disableUrlSsrfProtection = ssrfDisabled }
    return CimdMetadataFetcher(
      properties,
      UrlSecurity(internalProperties),
      mapper,
    )
  }

  private fun validDocument(
    clientId: String = "https://example.com/client",
    authMethod: String = "none",
    grantTypes: List<String> = listOf("authorization_code"),
    redirectUris: List<String> = listOf("https://example.com/callback"),
    clientName: String? = null,
    logoUri: String? = null,
  ): JsonNode {
    val node = mapper.createObjectNode()
    node.put("client_id", clientId)
    node.put("token_endpoint_auth_method", authMethod)
    node.set("grant_types", mapper.valueToTree<JsonNode>(grantTypes))
    node.set("redirect_uris", mapper.valueToTree<JsonNode>(redirectUris))
    if (clientName != null) node.put("client_name", clientName)
    if (logoUri != null) node.put("logo_uri", logoUri)
    return node
  }

  private fun expectedHash(
    clientIdUrl: String,
    redirectUris: List<String>,
  ): String {
    val material = clientIdUrl + "\n" + redirectUris.sorted().joinToString("\n")
    val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  @Test
  fun `rejects a non-https client id`() {
    val properties = OAuth2CimdProperties().apply { allowedHosts = listOf("example.com") }
    fetcher(ssrfDisabled = true, properties).fetchAndValidate("http://example.com/client").assert.isNull()
  }

  @Test
  fun `rejects a loopback client id under the SSRF guard`() {
    val properties = OAuth2CimdProperties().apply { allowedHosts = listOf("127.0.0.1") }
    fetcher(ssrfDisabled = false, properties).fetchAndValidate("https://127.0.0.1/client").assert.isNull()
  }

  @Test
  fun `rejects a host outside a configured allow-list`() {
    val properties = OAuth2CimdProperties().apply { allowedHosts = listOf("trusted.example.com") }
    fetcher(ssrfDisabled = true, properties).fetchAndValidate("https://evil.example.com/client").assert.isNull()
  }

  @Test
  fun `isSafeUrl allows any https host when the allow-list is empty`() {
    val properties = OAuth2CimdProperties().apply { allowedHosts = listOf() }
    val fetcher = fetcher(ssrfDisabled = true, properties)
    fetcher.isSafeUrl("https://example.com/client").assert.isTrue()
    fetcher.isSafeUrl("https://another-host.example.org/client").assert.isTrue()
  }

  @Test
  fun `buildClient accepts a well-formed document`() {
    val result = fetcher(ssrfDisabled = true).buildClient("https://example.com/client", validDocument())
    result.assert.isNotNull
    result!!
      .client.clientId.assert
      .isEqualTo("https://example.com/client")
    result.client.verified.assert
      .isFalse()
    result.client.name.assert
      .isEqualTo("https://example.com/client")
    result.client.redirectUris.assert
      .containsExactly("https://example.com/callback")
  }

  @Test
  fun `buildClient takes the client name from client_name when present`() {
    val document = validDocument(clientName = "Example Client")
    val result = fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)
    result!!
      .client.name.assert
      .isEqualTo("Example Client")
  }

  @Test
  fun `buildClient rejects a document whose client_id does not equal the requested url`() {
    val document = validDocument(clientId = "https://example.com/other")
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient rejects a non-textual client_id instead of throwing`() {
    // A malformed document can give client_id as an object/array; Jackson's asString() throws on those, so buildClient
    // must read it fail-closed and return null (buildClient runs outside fetchAndValidate's catch when called directly
    // in this test, so a throw would otherwise escape).
    val document = mapper.createObjectNode()
    document.set("client_id", mapper.createObjectNode().put("url", "https://example.com/client"))
    document.put("token_endpoint_auth_method", "none")
    document.set("grant_types", mapper.valueToTree<JsonNode>(listOf("authorization_code")))
    document.set("redirect_uris", mapper.valueToTree<JsonNode>(listOf("https://example.com/callback")))
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient rejects a non-public token_endpoint_auth_method`() {
    val document = validDocument(authMethod = "client_secret_basic")
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient rejects grant_types that lack authorization_code`() {
    val document = validDocument(grantTypes = listOf("refresh_token"))
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient rejects an empty redirect_uris`() {
    val document = validDocument(redirectUris = listOf())
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient rejects a redirect_uri on a foreign origin (open-redirect hijack)`() {
    val document = validDocument(redirectUris = listOf("https://evil.example.com/callback"))
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient rejects a same-host redirect_uri on a different explicit port`() {
    val document = validDocument(redirectUris = listOf("https://example.com:8443/callback"))
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient accepts a same-host redirect_uri that states the default https port explicitly`() {
    val document = validDocument(redirectUris = listOf("https://example.com:443/callback"))
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNotNull
  }

  @Test
  fun `buildClient rejects a same-host redirect_uri on a different scheme`() {
    val document = validDocument(redirectUris = listOf("http://example.com/callback"))
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient accepts a loopback redirect_uri alongside same-origin ones`() {
    val document =
      validDocument(
        redirectUris = listOf("https://example.com/callback", "http://localhost:8976/cb"),
      )
    val result = fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document)
    result.assert.isNotNull
    result!!.client.redirectUris.assert.containsExactlyInAnyOrder(
      "https://example.com/callback",
      "http://localhost:8976/cb",
    )
  }

  @Test
  fun `buildClient still rejects a foreign-origin https redirect alongside a loopback one`() {
    val document =
      validDocument(
        redirectUris = listOf("http://localhost:8976/cb", "https://evil.example.com/callback"),
      )
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient keeps logo_uri only when it is same-origin with the client id`() {
    val sameOrigin = validDocument(logoUri = "https://example.com/logo.png")
    val sameOriginResult = fetcher(ssrfDisabled = true).buildClient("https://example.com/client", sameOrigin)
    sameOriginResult!!.logoUri.assert.isEqualTo("https://example.com/logo.png")

    val foreignOrigin = validDocument(logoUri = "https://evil.example.com/logo.png")
    val foreignOriginResult = fetcher(ssrfDisabled = true).buildClient("https://example.com/client", foreignOrigin)
    foreignOriginResult!!.logoUri.assert.isNull()
  }

  @Test
  fun `buildClient leaves logoUri null when the document has none`() {
    val result = fetcher(ssrfDisabled = true).buildClient("https://example.com/client", validDocument())
    result!!.logoUri.assert.isNull()
  }

  @Test
  fun `metadataHash is a deterministic sha256 of the client id and sorted redirect uris`() {
    val clientIdUrl = "https://example.com/client"
    val redirectUris = listOf("https://example.com/callback")
    val result = fetcher(ssrfDisabled = true).buildClient(clientIdUrl, validDocument(redirectUris = redirectUris))

    val expected = expectedHash(clientIdUrl, redirectUris)
    result!!.metadataHash.assert.isEqualTo(expected)
    result.client.metadataHash.assert
      .isEqualTo(expected)
  }

  @Test
  fun `metadataHash changes when redirect_uris change (forces re-consent)`() {
    val first =
      fetcher(ssrfDisabled = true).buildClient("https://example.com/client", validDocument())
    val second =
      fetcher(ssrfDisabled = true).buildClient(
        "https://example.com/client",
        validDocument(redirectUris = listOf("https://example.com/callback", "https://example.com/callback2")),
      )
    first!!.metadataHash.assert.isNotEqualTo(second!!.metadataHash)
  }

  @Test
  fun `readCapped returns the body at exactly the size cap`() {
    val properties = OAuth2CimdProperties().apply { maxDocumentBytes = 16 }
    val body = ByteArray(16) { 'a'.code.toByte() }
    fetcher(ssrfDisabled = true, properties)
      .readCapped(body.inputStream())!!
      .size.assert
      .isEqualTo(16)
  }

  @Test
  fun `readCapped rejects a body one byte over the cap`() {
    val properties = OAuth2CimdProperties().apply { maxDocumentBytes = 16 }
    val body = ByteArray(17) { 'a'.code.toByte() }
    fetcher(ssrfDisabled = true, properties).readCapped(body.inputStream()).assert.isNull()
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
      fetcher.fetch("$base/ok", pinnedAddresses = null).assert.isNotNull
      fetcher.fetch("$base/notfound", pinnedAddresses = null).assert.isNull()
      // Redirects are disabled: an allow-listed host must not 302 us onto an internal address.
      fetcher.fetch("$base/redirect", pinnedAddresses = null).assert.isNull()
      redirectTargetHit.get().assert.isFalse()
    } finally {
      server.stop(0)
    }
  }

  @Test
  fun `fetch connects to the pinned addresses instead of re-resolving the host`() {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/ok") { ex ->
      val body = mapper.writeValueAsBytes(validDocument())
      ex.sendResponseHeaders(200, body.size.toLong())
      ex.responseBody.use { it.write(body) }
    }
    server.start()
    try {
      val url = "http://127.0.0.1:${server.address.port}/ok"
      val fetcher = fetcher(ssrfDisabled = true)

      // Pinned to the address the server actually listens on: succeeds.
      fetcher.fetch(url, pinnedAddresses = arrayOf(InetAddress.getByName("127.0.0.1"))).assert.isNotNull

      // Pinned to a different loopback address nothing listens on: the DNS resolver is expected to hand out
      // exactly this address rather than re-resolving "127.0.0.1" from the URL, so the connection must fail even
      // though the real host would have worked.
      fetcher.fetch(url, pinnedAddresses = arrayOf(InetAddress.getByName("127.0.0.2"))).assert.isNull()
    } finally {
      server.stop(0)
    }
  }

  @Test
  fun `buildClient rejects redirect_uris mixing a valid string with a container node`() {
    val document = mapper.createObjectNode()
    document.put("client_id", "https://example.com/client")
    document.put("token_endpoint_auth_method", "none")
    document.set("grant_types", mapper.valueToTree<JsonNode>(listOf("authorization_code")))
    document.set(
      "redirect_uris",
      mapper.createArrayNode().add("https://example.com/callback").add(mapper.createObjectNode()),
    )
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient rejects grant_types mixing a valid string with a container node`() {
    val document = mapper.createObjectNode()
    document.put("client_id", "https://example.com/client")
    document.put("token_endpoint_auth_method", "none")
    document.set("grant_types", mapper.createArrayNode().add("authorization_code").add(mapper.createObjectNode()))
    document.set("redirect_uris", mapper.valueToTree<JsonNode>(listOf("https://example.com/callback")))
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }

  @Test
  fun `buildClient rejects a grant_types that is present but not an array`() {
    val document = mapper.createObjectNode()
    document.put("client_id", "https://example.com/client")
    document.put("token_endpoint_auth_method", "none")
    document.put("grant_types", "authorization_code")
    document.set("redirect_uris", mapper.valueToTree<JsonNode>(listOf("https://example.com/callback")))
    fetcher(ssrfDisabled = true).buildClient("https://example.com/client", document).assert.isNull()
  }
}
