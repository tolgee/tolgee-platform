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

import io.tolgee.configuration.tolgee.OAuth2CimdProperties
import io.tolgee.util.Logging
import io.tolgee.util.UrlSecurity
import io.tolgee.util.logger
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.Timeout
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.InputStream
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.Base64

@Component
class CimdMetadataFetcher(
  private val properties: OAuth2CimdProperties,
  private val urlSecurity: UrlSecurity,
  private val objectMapper: ObjectMapper,
) : Logging {
  fun fetchAndValidate(clientIdUrl: String): CimdClient? {
    if (!isSafeUrl(clientIdUrl)) return null

    val document = fetch(clientIdUrl) ?: return null
    // Defense in depth: buildClient already reads every field fail-closed, but it builds a client out of untrusted
    // metadata, so any residual throw must still resolve to null rather than surface as a 500 on /oauth2/authorize.
    return try {
      buildClient(clientIdUrl, document)
    } catch (e: Exception) {
      logger.debug("CIMD document rejected for {}: {}", clientIdUrl, e.message)
      null
    }
  }

  private fun isSafeUrl(clientIdUrl: String): Boolean {
    val uri =
      try {
        URI(clientIdUrl)
      } catch (_: Exception) {
        return false
      }
    if (uri.scheme?.lowercase() != "https") return false
    val host = uri.host ?: return false
    if (properties.allowedHosts.isNotEmpty() &&
      properties.allowedHosts.none { it.equals(host, ignoreCase = true) }
    ) {
      return false
    }
    return try {
      urlSecurity.validateUrl(clientIdUrl)
      true
    } catch (_: Exception) {
      false
    }
  }

  internal fun fetch(clientIdUrl: String): JsonNode? {
    val host = URI(clientIdUrl).host ?: return null
    val pinned = validatedAddresses(host) ?: return null
    return try {
      pinnedClient(host, pinned).use { client ->
        client.execute(HttpGet(clientIdUrl).apply { addHeader("Accept", "application/json") }) { response ->
          if (response.code != 200) return@execute null
          val bytes = response.entity?.content?.use { readCapped(it) } ?: return@execute null
          objectMapper.readTree(bytes)
        }
      }
    } catch (e: Exception) {
      logger.debug("CIMD fetch failed for {}: {}", clientIdUrl, e.message)
      null
    }
  }

  // UrlSecurity has already vetted the URL, but it resolves DNS at validation time while the connection resolves
  // again at connect time; pinning the connection to these exact addresses is what closes that rebinding window.
  private fun validatedAddresses(host: String): Array<InetAddress>? =
    runCatching { InetAddress.getAllByName(host.removeSurrounding("[", "]")) }.getOrNull()

  private fun pinnedClient(
    host: String,
    addresses: Array<InetAddress>,
  ): CloseableHttpClient {
    val resolver =
      object : DnsResolver {
        override fun resolve(resolvedHost: String): Array<InetAddress> {
          if (!resolvedHost.equals(host, ignoreCase = true)) throw UnknownHostException(resolvedHost)
          return addresses
        }

        override fun resolveCanonicalHostname(resolvedHost: String): String = resolvedHost
      }
    val connectionManager =
      PoolingHttpClientConnectionManagerBuilder
        .create()
        .setDnsResolver(resolver)
        .setDefaultConnectionConfig(
          ConnectionConfig
            .custom()
            .setConnectTimeout(Timeout.ofMilliseconds(properties.fetchTimeoutMs))
            .setSocketTimeout(Timeout.ofMilliseconds(properties.fetchTimeoutMs))
            .build(),
        ).build()
    return HttpClients
      .custom()
      .setConnectionManager(connectionManager)
      .disableRedirectHandling()
      .disableAutomaticRetries()
      .build()
  }

  internal fun readCapped(stream: InputStream): ByteArray? {
    val max = properties.maxDocumentBytes.toInt()
    val buffer = stream.readNBytes(max + 1)
    if (buffer.size > max) return null
    return buffer
  }

  internal fun buildClient(
    clientIdUrl: String,
    document: JsonNode,
  ): CimdClient? {
    if (document.get("client_id").textOrNull() != clientIdUrl) return null
    if (document.get("token_endpoint_auth_method").textOrNull() != "none") return null

    val grantTypes = stringList(document.get("grant_types"))
    if (grantTypes.isNotEmpty() && !grantTypes.contains("authorization_code")) return null

    val redirectUris = stringList(document.get("redirect_uris"))
    if (redirectUris.isEmpty()) return null
    if (!redirectUris.all { isAcceptableRedirect(it, clientIdUrl) }) return null

    val client =
      OAuth2Client(
        clientId = clientIdUrl,
        name = document.get("client_name").textOrNull() ?: clientIdUrl,
        redirectUris = redirectUris,
        verified = false,
        metadataHash = metadataHash(clientIdUrl, redirectUris),
      )
    return CimdClient(client, sameOriginLogo(document, clientIdUrl), client.metadataHash!!)
  }

  private fun sameOriginLogo(
    document: JsonNode,
    clientIdUrl: String,
  ): String? {
    val logo = document.get("logo_uri").textOrNull() ?: return null
    if (!isSameOrigin(logo, clientIdUrl)) return null
    return logo
  }

  // Same-origin https, or a loopback (native MCP clients like Claude Code listen on localhost). Anything else —
  // a third https origin, a custom scheme — is where a hostile document would point a code, so it is refused.
  private fun isAcceptableRedirect(
    redirectUri: String,
    clientIdUrl: String,
  ): Boolean {
    if (isSameOrigin(redirectUri, clientIdUrl)) return true
    val uri = runCatching { URI(redirectUri) }.getOrNull() ?: return false
    if (uri.scheme != "http" && uri.scheme != "https") return false
    return OAuth2Client.isLoopbackHost(uri.host)
  }

  private fun isSameOrigin(
    redirectUri: String,
    clientIdUrl: String,
  ): Boolean {
    return try {
      val a = URI(redirectUri)
      val b = URI(clientIdUrl)
      a.scheme == b.scheme && a.host == b.host && effectivePort(a) == effectivePort(b)
    } catch (_: Exception) {
      false
    }
  }

  // https-only, so an omitted port (-1) is the same origin as an explicit :443.
  private fun effectivePort(uri: URI): Int {
    if (uri.port == -1) return 443
    return uri.port
  }

  private fun metadataHash(
    clientIdUrl: String,
    redirectUris: List<String>,
  ): String {
    val material = clientIdUrl + "\n" + redirectUris.sorted().joinToString("\n")
    val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  private fun stringList(node: JsonNode?): List<String> {
    if (node == null || !node.isArray) return emptyList()
    return node.mapNotNull { it.textOrNull() }
  }

  // Jackson's asString() coerces scalars but throws on a container node (object/array), so an attacker-supplied field
  // given as `{...}`/`[...]` would otherwise escape buildClient. Read every string field through this instead.
  private fun JsonNode?.textOrNull(): String? {
    val node = this ?: return null
    if (!node.isValueNode) return null
    return try {
      node.asString()
    } catch (_: Exception) {
      null
    }
  }
}
