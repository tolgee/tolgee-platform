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

package io.tolgee.security.oauth2.cimd

import io.tolgee.security.oauth2.OAuth2Client
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.security.MessageDigest
import java.util.Base64

/**
 * Resolves an unknown client presenting an HTTPS URL as its `client_id` into an unverified [CimdClient], by fetching
 * the metadata document at that URL and validating it fail-closed.
 *
 * Never throws: any hostile or malformed input returns null, because this runs on `/oauth2/authorize` and a throw
 * would turn a crafted document into a 500. Validation is deliberately strict — a rule not met drops the whole
 * document rather than dropping the offending field, which would silently narrow into a usable client.
 */
@Component
class CimdMetadataFetcher(
  private val documentFetcher: CimdDocumentFetcher,
) {
  private val mapper = JsonMapper.builder().build()

  fun fetchAndValidate(clientIdUrl: String): CimdClient? {
    val document = documentFetcher.fetch(clientIdUrl) ?: return null
    return buildClient(clientIdUrl, document)
  }

  internal fun buildClient(
    clientIdUrl: String,
    document: String,
  ): CimdClient? {
    val root = runCatching { mapper.readTree(document) }.getOrNull() ?: return null
    if (!root.isObject) return null

    if (textOrNull(root, "client_id") != clientIdUrl) return null
    if (textOrNull(root, "token_endpoint_auth_method") != "none") return null
    if (!grantTypesAllowAuthCode(root)) return null

    val clientOrigin = origin(clientIdUrl) ?: return null
    val redirectUris = validRedirectUris(root, clientOrigin) ?: return null

    val name = textOrNull(root, "client_name")?.takeIf { it.isNotBlank() } ?: displayOrigin(clientIdUrl)
    val logoUri = textOrNull(root, "logo_uri")?.takeIf { sameOrigin(it, clientOrigin) }

    val client =
      OAuth2Client(
        clientId = clientIdUrl,
        name = name,
        redirectUris = redirectUris,
        requiredScopes = emptyList(),
        verified = false,
        metadataHash = sha256(document),
      )
    return CimdClient(client, displayOrigin(clientIdUrl), logoUri)
  }

  private fun grantTypesAllowAuthCode(root: JsonNode): Boolean {
    val node = root.get("grant_types") ?: return true
    if (!node.isArray) return false
    for (i in 0 until node.size()) {
      val element = node.get(i)
      if (element.isValueNode && element.asString() == "authorization_code") return true
    }
    return false
  }

  /**
   * Every redirect must be same-origin HTTPS with the client_id or a loopback URI (RFC 8252 native apps), and carry no
   * fragment. A single bad element rejects the whole set — `mapNotNull` here would read as defensive but silently drop
   * the hostile entry and keep the rest.
   */
  private fun validRedirectUris(
    root: JsonNode,
    clientOrigin: String,
  ): List<String>? {
    val node = root.get("redirect_uris") ?: return null
    if (!node.isArray || node.isEmpty) return null
    val uris = mutableListOf<String>()
    for (i in 0 until node.size()) {
      val element = node.get(i)
      if (!element.isValueNode) return null
      val uri = element.asString()
      if (!isAllowedRedirect(uri, clientOrigin)) return null
      uris.add(uri)
    }
    return uris
  }

  private fun isAllowedRedirect(
    uri: String,
    clientOrigin: String,
  ): Boolean {
    val parsed = runCatching { URI(uri) }.getOrNull() ?: return false
    if (parsed.fragment != null) return false
    if (OAuth2Client.isLoopbackHost(parsed.host)) return true
    return parsed.scheme?.lowercase() == "https" && origin(uri) == clientOrigin
  }

  private fun sameOrigin(
    uri: String,
    clientOrigin: String,
  ): Boolean = origin(uri) == clientOrigin

  // scheme://host:port, the RFC 6454 origin — the comparison key for "same-origin". The default port is normalized so
  // an implicit port and its explicit form (https + :443, http + :80) compare as the same origin.
  private fun origin(uri: String): String? {
    val parsed = runCatching { URI(uri) }.getOrNull() ?: return null
    val scheme = parsed.scheme?.lowercase() ?: return null
    val host = parsed.host ?: return null
    val port = if (parsed.port != -1) parsed.port else defaultPort(scheme) ?: return null
    return "$scheme://$host:$port"
  }

  private fun defaultPort(scheme: String): Int? =
    when (scheme) {
      "https" -> 443
      "http" -> 80
      else -> null
    }

  // The human-facing origin the consent screen shows: no synthetic ":-1" when the port was left implicit.
  private fun displayOrigin(uri: String): String {
    val parsed = URI(uri)
    val base = "${parsed.scheme?.lowercase()}://${parsed.host}"
    if (parsed.port == -1) return base
    return "$base:${parsed.port}"
  }

  private fun textOrNull(
    node: JsonNode,
    field: String,
  ): String? {
    // asString throws on a container node in Jackson 3, so a non-value is treated as absent rather than crashing.
    val value = node.get(field) ?: return null
    if (!value.isValueNode) return null
    return value.asString()
  }

  private fun sha256(document: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(document.toByteArray(Charsets.UTF_8))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }
}
