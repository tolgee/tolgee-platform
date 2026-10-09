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
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

/**
 * Must stay byte-identical, in scheme host and path, to what `tolgee login` listens on in the tolgee-cli repo:
 * [OAuth2Client.allowsRedirectUri] compares all three literally, so a CLI that moved to `localhost` or another path
 * would stop being able to log in against every instance that never configured a redirect of its own.
 */
private const val DEFAULT_CLI_REDIRECT_URI = "http://127.0.0.1/callback"

/**
 * The clients Tolgee ships, built from `tolgee.oauth2.*`: the CLI, on by default and registered wherever the
 * authorization server is live, and the browser extension, off until an operator turns it on. Each has an on/off
 * flag and a redirect URI list with a built-in default. Clients nobody registered are [OAuth2ClientRegistry]'s job.
 */
@Component
class PreRegisteredOAuth2Clients(
  private val properties: OAuth2ServerProperties,
  private val oauth2IssuerResolver: OAuth2IssuerResolver,
) {
  /** The clients an operator asked for explicitly, which is what the startup guard is keyed on. */
  private val configured: List<OAuth2Client> = listOfNotNull(browserExtension(), configuredCli())

  val all: List<OAuth2Client> = configured + listOfNotNull(defaultCli())

  @PostConstruct
  fun requireIssuerForConfiguredClients() {
    if (configured.isEmpty()) return
    runCatching { oauth2IssuerResolver.issuerUrl }.onFailure {
      throw IllegalStateException(
        "tolgee.back-end-url (or tolgee.front-end-url) must be a usable issuer when a tolgee.oauth2 client is " +
          "configured: ${it.message}",
        it,
      )
    }
  }

  fun find(clientId: String): OAuth2Client? = all.firstOrNull { it.clientId == clientId }

  /** Off until an operator turns it on; a configured redirect list replaces the published extension's URIs. */
  private fun browserExtension(): OAuth2Client? {
    if (!properties.browserExtensionEnabled) return null
    val redirectUris =
      properties.browserExtensionRedirectUris.ifEmpty { OAuth2Constants.OFFICIAL_BROWSER_EXTENSION_REDIRECT_URIS }
    return OAuth2Client(
      clientId = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
      name = "Tolgee Browser Extension",
      redirectUris = requireValidRedirectUris(redirectUris),
      requiredScopes = listOf(Scope.KEYS_VIEW, Scope.TRANSLATIONS_VIEW),
    )
  }

  private fun configuredCli(): OAuth2Client? {
    if (!properties.cliEnabled) return null
    if (properties.cliRedirectUris.isEmpty()) return null
    return cliClient(requireValidRedirectUris(properties.cliRedirectUris))
  }

  /** Registered wherever the authorization server is live: `tolgee login` must work on an unconfigured instance. */
  private fun defaultCli(): OAuth2Client? {
    if (!properties.cliEnabled) return null
    if (properties.cliRedirectUris.isNotEmpty()) return null
    if (!oauth2IssuerResolver.isConfigured) return null
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
