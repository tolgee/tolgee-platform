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
import io.tolgee.security.oauth2.cimd.CimdClient
import io.tolgee.security.oauth2.cimd.CimdClientCache
import io.tolgee.security.oauth2.cimd.CimdClientPolicy
import io.tolgee.security.oauth2.cimd.CimdResolution
import org.springframework.stereotype.Component
import java.net.URI

/**
 * The OAuth clients Tolgee will issue tokens to: the ones it ships ([PreRegisteredOAuth2Clients]), and any unknown
 * client that presents a valid Client ID Metadata Document (CIMD) at an HTTPS `client_id` URL.
 */
@Component
class OAuth2ClientRegistry(
  private val preRegisteredClients: PreRegisteredOAuth2Clients,
  private val cimdClientCache: CimdClientCache,
  private val cimdClientPolicy: CimdClientPolicy,
  private val oauth2IssuerResolver: OAuth2IssuerResolver,
) {
  fun find(clientId: String): OAuth2Client? = findPreRegistered(clientId) ?: findCimd(clientId)?.client

  /** The CIMD path only: null for a pre-registered id or a non-URL id. May fetch the document. */
  fun findCimd(clientId: String): CimdClient? {
    if (!isCimdCandidate(clientId)) return null
    return cimdClientCache.get(clientId)
  }

  /**
   * The client a token request, or a consent screen for an already-created grant, claims to be. A CIMD client
   * nothing has read yet still gets one — bare, unverified, with no redirect URIs.
   *
   * This must never fetch: only [io.tolgee.security.oauth2.cimd.CimdDocumentCheck] reads a document for a client
   * that already holds a grant, and only behind a validated refresh token. See `docs/oauth/README.md`.
   */
  fun findForExistingGrant(clientId: String): OAuth2Client? {
    findPreRegistered(clientId)?.let { return it }
    if (!isCimdCandidate(clientId)) return null
    cimdClientCache.cachedClient(clientId)?.let { return it.client }
    return unresolvableCimdClient(clientId)
  }

  /**
   * Reads the document now on the [io.tolgee.security.oauth2.cimd.CimdFetchLane.GRANT_CHECK] lane. Only
   * [io.tolgee.security.oauth2.cimd.CimdDocumentCheck] may call it, and only for an id [servesCimdClient] accepts.
   * Null means this server had no room for the fetch.
   */
  fun resolveForCheck(clientIdUrl: String): CimdResolution? = cimdClientCache.fetchOnGrantLane(clientIdUrl)

  /** A `client_id` only the CIMD path could serve: not pre-registered, and past the local candidate policy. */
  fun servesCimdClient(clientId: String): Boolean = isCimdCandidate(clientId)

  /**
   * Whether this instance serves the client at all: a pre-registered id, or an id the CIMD path would still accept
   * today. It answers nothing about grants, consent or withdrawal. The paths that run on **every** request ask it
   * because a pre-registered id an operator has since removed, or an id the policy no longer accepts, leaves
   * nothing to honour a grant against.
   */
  fun servesClient(clientId: String): Boolean {
    findPreRegistered(clientId)?.let { return true }
    return isCimdCandidate(clientId)
  }

  private fun isCimdCandidate(clientId: String): Boolean =
    oauth2IssuerResolver.isConfigured && findPreRegistered(clientId) == null && cimdClientPolicy.isCandidate(clientId)

  private fun unresolvableCimdClient(clientId: String) =
    OAuth2Client(
      clientId = clientId,
      name = clientId,
      redirectUris = emptyList(),
      verified = false,
      hasMetadataDocument = true,
    )

  private fun findPreRegistered(clientId: String): OAuth2Client? = preRegisteredClients.find(clientId)
}

/** A client Tolgee issues tokens to. Every client is public, must use PKCE, and always goes through consent. */
data class OAuth2Client(
  val clientId: String,
  val name: String,
  val redirectUris: List<String>,
  val requiredScopes: List<Scope> = emptyList(),
  val verified: Boolean = true,
  val metadataHash: String? = null,
  /**
   * Whether this client identifies itself with a metadata document, which is what its grants are kept alive by.
   * Not the same as [verified], which is only the consent screen's trust badge.
   */
  val hasMetadataDocument: Boolean = false,
) {
  fun allowsRedirectUri(redirectUri: String): Boolean {
    if (UrlOrigins.parse(redirectUri) == null) return false
    if (redirectUri in redirectUris) return true
    return redirectUris.any { matchesLoopback(it, redirectUri) }
  }

  /**
   * RFC 8252 §7.3: a loopback redirect must be accepted on whatever port the client got from the OS at request time,
   * so the registered port is not part of the comparison. Everything else — scheme, host, path, and any non-loopback
   * URI — still has to match exactly.
   */
  private fun matchesLoopback(
    registered: String,
    presented: String,
  ): Boolean {
    val registeredUri = UrlOrigins.parse(registered) ?: return false
    if (!isLoopbackHost(registeredUri.host)) return false
    val presentedUri = UrlOrigins.parse(presented) ?: return false
    return presentedUri.scheme == registeredUri.scheme &&
      presentedUri.host == registeredUri.host &&
      presentedUri.path.orEmpty() == registeredUri.path.orEmpty() &&
      presentedUri.userInfo == registeredUri.userInfo &&
      presentedUri.query == null &&
      presentedUri.fragment == null
  }

  companion object {
    // URI.getHost() renders an IPv6 literal with its brackets.
    private val LOOPBACK_HOSTS = setOf("127.0.0.1", "[::1]", "localhost")

    internal fun isLoopbackHost(host: String?): Boolean = host in LOOPBACK_HOSTS

    /** Whether the code goes to something listening on the user's own machine rather than to a website. */
    fun redirectsToLocalApp(redirectUri: String): Boolean = isLoopbackHost(UrlOrigins.parse(redirectUri)?.host)

    /**
     * The spelling two registered redirects share exactly when [allowsRedirectUri] accepts the same presented URIs
     * for both. A loopback entry's port only matters when that entry also carries a query: [matchesLoopback] ignores
     * both, so the exact-match lane is then the only one that can accept it.
     */
    internal fun redirectEquivalenceKey(uri: String): String {
      val parsed = UrlOrigins.parse(uri) ?: return uri
      if (!isLoopbackHost(parsed.host)) return uri
      if (parsed.query != null || parsed.port == -1) return uri
      val userInfo = parsed.userInfo?.let { "$it@" }.orEmpty()
      return "${parsed.scheme}://$userInfo${parsed.host}${parsed.path.orEmpty()}"
    }
  }
}
