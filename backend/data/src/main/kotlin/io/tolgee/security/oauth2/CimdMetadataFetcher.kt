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

import io.tolgee.model.enums.Scope
import io.tolgee.util.Logging
import io.tolgee.util.UrlSecurity
import io.tolgee.util.logger
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64

/**
 * Fetches and validates a Client ID Metadata Document (HTTPS-URL `client_id`) into a transient [RegisteredClient]. The
 * generated `id` hashes client id + redirect uris, so a redirect change forces re-consent.
 */
@Component
class CimdMetadataFetcher(
  private val properties: OAuth2CimdProperties,
  private val serverProperties: OAuth2ServerProperties,
  private val urlSecurity: UrlSecurity,
  private val objectMapper: ObjectMapper,
) : Logging {
  private val httpClient: HttpClient by lazy {
    HttpClient
      .newBuilder()
      .followRedirects(HttpClient.Redirect.NEVER)
      .connectTimeout(Duration.ofMillis(properties.fetchTimeoutMs))
      .build()
  }

  fun fetchAndValidate(clientIdUrl: String): RegisteredClient? {
    if (!properties.enabled) return null
    // Fail closed: the SSRF guard resolves DNS at validation time but java.net.http re-resolves at connect time, so a
    // rebinding host could still be reached. Until the fetch pins the validated IP, an explicit allow-list is required.
    if (properties.allowedHosts.isEmpty()) return null
    if (!isSafeUrl(clientIdUrl)) return null

    val document = fetch(clientIdUrl) ?: return null
    return buildClient(clientIdUrl, document)
  }

  private fun isSafeUrl(clientIdUrl: String): Boolean {
    val uri =
      try {
        URI(clientIdUrl)
      } catch (_: Exception) {
        return false
      }
    if (uri.scheme?.lowercase() != "https") return false
    if (uri.host?.lowercase() !in properties.allowedHosts.map { it.lowercase() }) return false
    return try {
      urlSecurity.validateUrl(clientIdUrl)
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun fetch(clientIdUrl: String): JsonNode? {
    return try {
      val request =
        HttpRequest
          .newBuilder(URI(clientIdUrl))
          .timeout(Duration.ofMillis(properties.fetchTimeoutMs))
          .header("Accept", "application/json")
          .GET()
          .build()
      val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
      if (response.statusCode() != 200) return null
      val bytes = response.body().use { readCapped(it) } ?: return null
      objectMapper.readTree(bytes)
    } catch (e: Exception) {
      logger.debug("CIMD fetch failed for {}: {}", clientIdUrl, e.message)
      null
    }
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
  ): RegisteredClient? {
    if (document.get("client_id")?.asString() != clientIdUrl) return null
    if (document.get("token_endpoint_auth_method")?.asString() != "none") return null

    val grantTypes = stringList(document.get("grant_types"))
    if (grantTypes.isNotEmpty() && !grantTypes.contains("authorization_code")) return null

    val redirectUris = stringList(document.get("redirect_uris"))
    if (redirectUris.isEmpty()) return null
    if (!redirectUris.all { isSameOrigin(it, clientIdUrl) }) return null

    val builder =
      RegisteredClient
        .withId(deterministicId(clientIdUrl, redirectUris))
        .clientId(clientIdUrl)
        .clientName(document.get("client_name")?.asString() ?: clientIdUrl)
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .clientSettings(
          ClientSettings
            .builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(true)
            .build(),
        ).tokenSettings(serverProperties.tokenSettings())
    // Only grant refresh tokens if the document didn't opt out (empty = unspecified, or explicitly listed).
    if (grantTypes.isEmpty() || grantTypes.contains("refresh_token")) {
      builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
    }
    redirectUris.forEach { builder.redirectUri(it) }
    Scope.entries.forEach { builder.scope(it.value) }
    return builder.build()
  }

  private fun isSameOrigin(
    redirectUri: String,
    clientIdUrl: String,
  ): Boolean {
    return try {
      val a = URI(redirectUri)
      val b = URI(clientIdUrl)
      a.scheme == b.scheme && a.host == b.host && a.port == b.port
    } catch (_: Exception) {
      false
    }
  }

  private fun deterministicId(
    clientIdUrl: String,
    redirectUris: List<String>,
  ): String {
    val material = clientIdUrl + "\n" + redirectUris.sorted().joinToString("\n")
    val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
    return "cimd_" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  private fun stringList(node: JsonNode?): List<String> {
    if (node == null || !node.isArray) return emptyList()
    return node.mapNotNull { it.asString() }
  }
}
