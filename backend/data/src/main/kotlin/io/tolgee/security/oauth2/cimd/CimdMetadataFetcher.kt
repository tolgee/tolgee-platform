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
import io.tolgee.security.oauth2.UrlOrigins
import io.tolgee.security.oauth2.lowercaseScheme
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.util.Base64

/**
 * Resolves an unknown client presenting an HTTPS URL as its `client_id` into an unverified [CimdClient]. No
 * document can make it throw, because this runs on `/oauth2/authorize` and a throw would turn a crafted document
 * into a 500. It does propagate [CimdNoCapacityException] from the fetcher, which says nothing about the client and
 * which every caller must catch.
 */
@Component
class CimdMetadataFetcher(
  private val documentFetcher: CimdDocumentFetcher,
) : Logging {
  private val mapper = JsonMapper.builder().build()

  fun fetchAndValidate(
    clientIdUrl: String,
    forExistingGrant: Boolean = false,
  ): CimdResolution {
    return when (val document = documentFetcher.fetch(clientIdUrl, forExistingGrant)) {
      is CimdDocument.Body ->
        buildClient(clientIdUrl, document.content)?.let { CimdResolution.Resolved(it) } ?: CimdResolution.Rejected
      CimdDocument.Gone -> CimdResolution.Withdrawn
      CimdDocument.Rejected -> CimdResolution.Rejected
      CimdDocument.Unavailable -> CimdResolution.Unavailable
    }
  }

  internal fun buildClient(
    clientIdUrl: String,
    document: String,
  ): CimdClient? {
    val root = runCatching { mapper.readTree(document) }.getOrNull() ?: return reject(clientIdUrl, "unparseable JSON")
    if (!root.isObject) return reject(clientIdUrl, "document root is not an object")

    if (textOrNull(root, "client_id") != clientIdUrl) {
      return reject(clientIdUrl, "client_id does not match the URL the document was fetched from")
    }
    if (textOrNull(root, "token_endpoint_auth_method") != "none") {
      return reject(clientIdUrl, "token_endpoint_auth_method is not \"none\"")
    }
    val grantTypes = grantTypes(root) ?: return reject(clientIdUrl, "grant_types does not allow authorization_code")

    val clientOrigin =
      UrlOrigins.originOf(clientIdUrl) ?: return reject(clientIdUrl, "client_id has no parseable origin")
    val redirectUris =
      validRedirectUris(root, clientOrigin)
        ?: return reject(clientIdUrl, "redirect_uris is absent, empty, or has an entry that is not same-origin https")

    val displayOrigin = displayOrigin(clientIdUrl)
    val name = displayName(root) ?: displayOrigin

    val client =
      OAuth2Client(
        clientId = clientIdUrl,
        name = name,
        redirectUris = redirectUris,
        requiredScopes = emptyList(),
        verified = false,
        hasMetadataDocument = true,
        metadataHash = consentedTermsHash(clientIdUrl, redirectUris, grantTypes),
      )
    return CimdClient(client, displayOrigin)
  }

  private fun reject(
    clientIdUrl: String,
    reason: String,
  ): CimdClient? {
    logger.info("CIMD document refused for {}: {}", clientIdUrl, reason)
    return null
  }

  private fun displayName(root: JsonNode): String? {
    val name = textOrNull(root, "client_name")?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (name.length > MAX_CLIENT_NAME_LENGTH) return null
    if (name.codePoints().anyMatch { it.isFormattingControl() || it.isCombiningMark() }) return null
    return name
  }

  /** Code points, not `Char`s: half a surrogate pair types as SURROGATE, so both filters would read it as safe. */
  private fun Int.isFormattingControl(): Boolean =
    when (Character.getType(this).toByte()) {
      Character.CONTROL, Character.FORMAT, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR -> true
      else -> false
    }

  /**
   * Stacked combining marks render above their own line box, so a name made of them paints over whatever sits
   * above it — on the consent screen, the warning that says this app is unverified. A length cap does not bound
   * that: the ink goes up, not along.
   */
  private fun Int.isCombiningMark(): Boolean =
    when (Character.getType(this).toByte()) {
      Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK -> true
      else -> false
    }

  /**
   * Covers only what the authorize path acts on, so an edit that cannot change whether a redirect is accepted does
   * not read as drift. Versioned; [io.tolgee.security.oauth2.OAuth2AuthorizationService] reads the version.
   */
  private fun consentedTermsHash(
    clientIdUrl: String,
    redirectUris: List<String>,
    grantTypes: List<String>,
  ): String =
    HASH_SCHEME_PREFIX +
      sha256(
        listOf(
          clientIdUrl,
          redirectUris.map { OAuth2Client.redirectEquivalenceKey(it) }.sorted().joinToString(" "),
          grantTypes.filter { it in ACTED_ON_GRANT_TYPES }.sorted().joinToString(" "),
        ).joinToString("\n"),
      )

  /** An absent field resolves to RFC 7591's default, so writing that default out explicitly is not a change. */
  private fun grantTypes(root: JsonNode): List<String>? {
    val node = root.get("grant_types") ?: return listOf(AUTHORIZATION_CODE)
    val types = stringArray(node) ?: return null
    if (!types.contains(AUTHORIZATION_CODE)) return null
    return types
  }

  private fun stringArray(node: JsonNode): List<String>? {
    if (!node.isArray) return null
    val values = mutableListOf<String>()
    for (i in 0 until node.size()) {
      val element = node.get(i)
      if (!element.isValueNode) return null
      values.add(element.asString())
    }
    return values
  }

  private fun validRedirectUris(
    root: JsonNode,
    clientOrigin: String,
  ): List<String>? {
    val node = root.get("redirect_uris") ?: return null
    if (!node.isArray || node.isEmpty) return null
    if (node.size() > MAX_REDIRECT_URIS) return null
    val uris = stringArray(node) ?: return null
    if (uris.any { it.length > MAX_URI_LENGTH || !isAllowedRedirect(it, clientOrigin) }) return null
    return uris
  }

  private fun isAllowedRedirect(
    uri: String,
    clientOrigin: String,
  ): Boolean {
    val parsed = UrlOrigins.parse(uri) ?: return false
    if (parsed.fragment != null) return false
    val scheme = parsed.lowercaseScheme ?: return false
    if (OAuth2Client.isLoopbackHost(parsed.host)) return scheme == "http" || scheme == "https"
    return scheme == "https" && UrlOrigins.originOf(uri) == clientOrigin
  }

  private fun displayOrigin(uri: String): String {
    val parsed = UrlOrigins.parse(uri) ?: return uri
    val base = "${parsed.lowercaseScheme}://${parsed.host?.lowercase()}"
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

  private fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  companion object {
    const val MAX_CLIENT_NAME_LENGTH = 100
    const val MAX_REDIRECT_URIS = 20
    const val MAX_URI_LENGTH = 2000
    const val HASH_SCHEME_PREFIX = "v1:"
    private const val AUTHORIZATION_CODE = "authorization_code"

    /** The only entry in `grant_types` the authorize path reads; adding or removing any other must not read as drift. */
    private val ACTED_ON_GRANT_TYPES = setOf(AUTHORIZATION_CODE)
  }
}
