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
import org.springframework.stereotype.Component
import java.net.URI

/**
 * A client Tolgee issues tokens to. Every client is public (no secret), must use PKCE, and always goes through the
 * consent screen; the only per-client facts are its redirect URIs and which scopes the screen locks as required.
 */
data class OAuth2Client(
  val clientId: String,
  val name: String,
  val redirectUris: List<String>,
  val requiredScopes: List<Scope> = emptyList(),
) {
  fun allowsRedirectUri(redirectUri: String): Boolean {
    // Parsed, not merely compared: a redirect that matches only as a string is still built into a Location header
    // with URI.create, so a misconfigured entry would 500 the flow instead of simply never matching.
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
    if (registeredUri.host !in LOOPBACK_HOSTS) return false
    val presentedUri = parse(presented) ?: return false
    return presentedUri.scheme == registeredUri.scheme &&
      presentedUri.host == registeredUri.host &&
      presentedUri.path.orEmpty() == registeredUri.path.orEmpty() &&
      presentedUri.query == null &&
      presentedUri.fragment == null
  }

  private fun parse(uri: String): URI? = runCatching { URI(uri) }.getOrNull()

  companion object {
    // URI.getHost() renders an IPv6 literal with its brackets.
    internal val LOOPBACK_HOSTS = setOf("127.0.0.1", "[::1]")
  }
}

/**
 * The clients Tolgee ships, built from configuration. A client with no redirect URIs configured is absent, which is
 * how an operator switches it off; nothing is persisted, so no stale registration can outlive that intent.
 */
@Component
class OAuth2ClientRegistry(
  private val properties: OAuth2ServerProperties,
) {
  val clients: List<OAuth2Client>
    get() = listOfNotNull(browserExtension(), cli())

  fun find(clientId: String): OAuth2Client? = clients.firstOrNull { it.clientId == clientId }

  /** Whether a grant issued to [clientId] may still authenticate. */
  fun isStillAuthorized(clientId: String): Boolean = find(clientId) != null

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
      if (parsed.scheme != "https" && parsed.host !in OAuth2Client.LOOPBACK_HOSTS) {
        throw IllegalStateException(
          "tolgee.oauth2 redirect URI must use https unless it is a loopback address, got: $uri",
        )
      }
    }
    return uris
  }
}
