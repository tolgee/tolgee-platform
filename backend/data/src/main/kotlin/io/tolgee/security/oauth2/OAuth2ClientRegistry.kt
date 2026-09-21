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
 * Must stay byte-identical, in scheme host and path, to what `tolgee login` listens on in the tolgee-cli repo:
 * [OAuth2Client.allowsRedirectUri] compares all three literally, so a CLI that moved to `localhost` or another path
 * would stop being able to log in against every instance that never configured a redirect of its own.
 */
private const val DEFAULT_CLI_REDIRECT_URI = "http://127.0.0.1/callback"

/**
 * The OAuth clients Tolgee will issue tokens to: the CLI, which is registered wherever the authorization server is
 * live, the browser extension where an operator configured its redirect URI, and any unknown client that presents a
 * valid Client ID Metadata Document (CIMD) at an HTTPS `client_id` URL.
 */
@Component
class OAuth2ClientRegistry(
  private val properties: OAuth2ServerProperties,
  private val cimdClientCache: CimdClientCache,
  private val cimdClientPolicy: CimdClientPolicy,
  private val issuerResolver: OAuth2IssuerResolver,
) {
  private val configuredClients: List<OAuth2Client> = listOfNotNull(browserExtension(), configuredCli())

  val clients: List<OAuth2Client> = configuredClients + listOfNotNull(defaultCli())

  @PostConstruct
  fun requireIssuerForPreRegisteredClients() {
    if (configuredClients.isEmpty()) return
    runCatching { issuerResolver.issuerUrl }.onFailure {
      throw IllegalStateException(
        "tolgee.back-end-url (or tolgee.front-end-url) must be a usable issuer when a tolgee.oauth2 client is " +
          "configured: ${it.message}",
        it,
      )
    }
  }

  fun find(clientId: String): OAuth2Client? = findPreRegistered(clientId) ?: findCimd(clientId)?.client

  /** The CIMD path only: null for a pre-registered id or a non-URL id. Used by the consent screen for the logo. */
  fun findCimd(clientId: String): CimdClient? {
    if (!isCimdCandidate(clientId)) return null
    return cimdClientCache.get(clientId)
  }

  /**
   * The client a token request, or a consent screen for an already-created grant, claims to be. A CIMD client whose
   * document does not resolve on this particular hop still gets one — bare, unverified, with no redirect URIs — so a
   * third party's blip cannot kill a grant, or strand a browser on a consent screen that 404s.
   */
  fun findForExistingGrant(clientId: String): OAuth2Client? {
    find(clientId)?.let { return it }
    if (!isStillAuthorized(clientId)) return null
    return unresolvableCimdClient(clientId)
  }

  fun isStillAuthorized(clientId: String): Boolean = findPreRegistered(clientId) != null || isCimdCandidate(clientId)

  /** A client_id only the CIMD path could serve: not pre-registered, and past the local candidate policy. */
  private fun isCimdCandidate(clientId: String): Boolean =
    issuerResolver.isConfigured && findPreRegistered(clientId) == null && cimdClientPolicy.isCandidate(clientId)

  private fun unresolvableCimdClient(clientId: String) =
    OAuth2Client(clientId = clientId, name = clientId, redirectUris = emptyList(), verified = false)

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

  private fun configuredCli(): OAuth2Client? {
    if (!properties.cliEnabled) return null
    if (properties.cliRedirectUris.isEmpty()) return null
    return cliClient(requireValidRedirectUris(properties.cliRedirectUris))
  }

  /**
   * `tolgee login` has to work against an instance nobody configured for it, so the CLI is registered wherever the
   * authorization server is live at all. It is left out where the issuer does not resolve, because then no OAuth
   * endpoint answers anyway.
   */
  private fun defaultCli(): OAuth2Client? {
    if (!properties.cliEnabled) return null
    if (properties.cliRedirectUris.isNotEmpty()) return null
    if (!issuerResolver.isConfigured) return null
    return cliClient(listOf(DEFAULT_CLI_REDIRECT_URI))
  }

  private fun cliClient(redirectUris: List<String>) =
    OAuth2Client(
      clientId = OAuth2Constants.CLI_CLIENT_ID,
      name = "Tolgee CLI",
      redirectUris = redirectUris,
    )

  /**
   * OAuth 2.1 §2.3 and §1.5: a registered redirect URI must be absolute, carry no fragment, and use https unless it
   * is a loopback address. A relative entry is the dangerous one — it parses, matches, and then resolves against
   * Tolgee's own origin, so the authorization code would be delivered back to Tolgee instead of to the client.
   */
  private fun requireValidRedirectUris(uris: List<String>): List<String> {
    uris.forEach { uri ->
      val parsed =
        UrlOrigins.parse(uri)?.takeIf { it.isAbsolute }
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
 */
data class OAuth2Client(
  val clientId: String,
  val name: String,
  val redirectUris: List<String>,
  val requiredScopes: List<Scope> = emptyList(),
  val verified: Boolean = true,
  val metadataHash: String? = null,
  /**
   * Whether this client identifies itself with a metadata document, which is what its grants are kept alive by.
   * Deliberately separate from [verified]: that one is the consent screen's trust badge, and an operator marking a
   * publisher as trusted must not also switch off the checks that let that publisher retire the client.
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

    /**
     * The spelling two registered redirects share exactly when they accept the same presented URIs, so a projection
     * of the consented terms can be built on the equivalence [allowsRedirectUri] actually honours instead of guessing
     * at it.
     *
     * A loopback entry is accepted through two lanes: an exact string match, and [matchesLoopback], which ignores the
     * port — and the entry's own query, which it never reads. The port therefore only ever matters through the exact
     * lane, and that lane accepts nothing the loopback lane does not already accept unless the entry carries a query.
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
