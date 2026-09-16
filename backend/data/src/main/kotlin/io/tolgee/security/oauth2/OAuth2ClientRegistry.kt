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

import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.model.enums.Scope
import io.tolgee.security.oauth2.cimd.CimdClient
import io.tolgee.security.oauth2.cimd.CimdClientCache
import io.tolgee.security.oauth2.cimd.CimdClientPolicy
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.net.URI

/**
 * The OAuth clients Tolgee will issue tokens to: the pre-registered ones it ships (the browser extension and CLI,
 * from configuration) plus any unknown client that presents a valid Client ID Metadata Document (CIMD) at an HTTPS
 * `client_id` URL. A pre-registered id always wins, so a seeded client can never be shadowed by the CIMD path.
 */
@Component
class OAuth2ClientRegistry(
  private val properties: OAuth2ServerProperties,
  private val cimdClientCache: CimdClientCache,
  private val cimdClientPolicy: CimdClientPolicy,
  private val issuerResolver: OAuth2IssuerResolver,
) {
  val clients: List<OAuth2Client> = listOfNotNull(browserExtension(), cli())

  /** Issuer-based, so an instance with a usable issuer accepts CIMD clients even before any client is pre-registered. */
  val isEnabled: Boolean
    get() = issuerResolver.isConfigured

  // A pre-registered client cannot function without an issuer, so its presence makes the issuer mandatory even though
  // enabling is otherwise issuer-based.
  @PostConstruct
  fun requireIssuerForPreRegisteredClients() {
    if (clients.isNotEmpty()) issuerResolver.issuerUrl
  }

  fun find(clientId: String): OAuth2Client? = findPreRegistered(clientId) ?: findCimd(clientId)?.client

  /** The CIMD path only: null for a pre-registered id or a non-URL id. Used by the consent screen for the logo. */
  fun findCimd(clientId: String): CimdClient? {
    if (findPreRegistered(clientId) != null) return null
    if (!cimdClientPolicy.isCandidate(clientId)) return null
    return cimdClientCache.get(clientId)
  }

  fun isStillAuthorized(clientId: String): Boolean {
    if (findPreRegistered(clientId) != null) return true
    // A CIMD grant must not depend on re-fetching the third-party document (its uptime), so this never fetches; only
    // the local candidate policy is re-checked. Metadata drift is caught by the hash at code exchange and refresh.
    return cimdClientPolicy.isCandidate(clientId)
  }

  private fun findPreRegistered(clientId: String): OAuth2Client? = clients.firstOrNull { it.clientId == clientId }

  private fun browserExtension(): OAuth2Client? {
    if (properties.browserExtensionRedirectUris.isEmpty()) return null
    return OAuth2Client(
      clientId = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
      name = "Tolgee Browser Extension",
      redirectUris = requireValidRedirectUris(properties.browserExtensionRedirectUris),
      requiredScopes = listOf(Scope.KEYS_VIEW, Scope.TRANSLATIONS_VIEW),
    )
  }

  private fun cli(): OAuth2Client? {
    if (properties.cliRedirectUris.isEmpty()) return null
    return OAuth2Client(
      clientId = OAuth2Constants.CLI_CLIENT_ID,
      name = "Tolgee CLI",
      redirectUris = requireValidRedirectUris(properties.cliRedirectUris),
    )
  }

  /**
   * OAuth 2.1 §2.3 and §1.5: a registered redirect URI must be absolute, carry no fragment, and use https unless it
   * is a loopback address. A relative entry is the dangerous one — it parses, matches, and then resolves against
   * Tolgee's own origin, so the authorization code would be delivered back to Tolgee instead of to the client.
   */
  private fun requireValidRedirectUris(uris: List<String>): List<String> {
    uris.forEach { uri ->
      val parsed =
        runCatching { URI(uri) }.getOrNull()?.takeIf { it.isAbsolute }
          ?: throw IllegalStateException("tolgee.oauth2 redirect URI must be an absolute URL, got: $uri")
      if (parsed.fragment != null) {
        throw IllegalStateException("tolgee.oauth2 redirect URI must not carry a fragment, got: $uri")
      }
      if (parsed.scheme != "https" && !OAuth2Client.isLoopbackHost(parsed.host)) {
        throw IllegalStateException(
          "tolgee.oauth2 redirect URI must use https unless it is a loopback address, got: $uri",
        )
      }
    }
    return uris
  }
}

/**
 * A client Tolgee issues tokens to. Every client is public (no secret), must use PKCE, and always goes through the
 * consent screen; the only per-client facts are its redirect URIs and which scopes the screen locks as required.
 *
 * A pre-registered client is [verified] and carries no [metadataHash]. A client resolved from a Client ID Metadata
 * Document (CIMD) is not verified — the consent screen warns the user — and carries the hash of the document it was
 * built from, so a later change to that document can invalidate grants issued against the old one.
 */
data class OAuth2Client(
  val clientId: String,
  val name: String,
  val redirectUris: List<String>,
  val requiredScopes: List<Scope> = emptyList(),
  val verified: Boolean = true,
  val metadataHash: String? = null,
) {
  fun allowsRedirectUri(redirectUri: String): Boolean {
    if (parse(redirectUri) == null) return false
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
    val registeredUri = parse(registered) ?: return false
    if (!isLoopbackHost(registeredUri.host)) return false
    val presentedUri = parse(presented) ?: return false
    return presentedUri.scheme == registeredUri.scheme &&
      presentedUri.host == registeredUri.host &&
      presentedUri.path.orEmpty() == registeredUri.path.orEmpty() &&
      presentedUri.userInfo == registeredUri.userInfo &&
      presentedUri.query == null &&
      presentedUri.fragment == null
  }

  private fun parse(uri: String): URI? = runCatching { URI(uri) }.getOrNull()

  companion object {
    // URI.getHost() renders an IPv6 literal with its brackets.
    private val LOOPBACK_HOSTS = setOf("127.0.0.1", "[::1]", "localhost")

    internal fun isLoopbackHost(host: String?): Boolean = host in LOOPBACK_HOSTS
  }
}
