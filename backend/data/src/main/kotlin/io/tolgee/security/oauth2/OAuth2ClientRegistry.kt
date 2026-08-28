package io.tolgee.security.oauth2

import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.model.enums.Scope
import org.springframework.stereotype.Component

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
  fun allowsRedirectUri(redirectUri: String): Boolean = redirectUri in redirectUris

  fun allowsScope(scope: String): Boolean = scope in ALLOWED_SCOPES

  companion object {
    private val ALLOWED_SCOPES = Scope.entries.map { it.value }.toSet()
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

  private fun browserExtension(): OAuth2Client? {
    if (properties.browserExtensionRedirectUris.isEmpty()) return null
    return OAuth2Client(
      clientId = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
      name = "Tolgee Browser Extension",
      redirectUris = properties.browserExtensionRedirectUris,
      requiredScopes = listOf(Scope.KEYS_VIEW, Scope.TRANSLATIONS_VIEW),
    )
  }

  private fun cli(): OAuth2Client? {
    if (properties.cliRedirectUris.isEmpty()) return null
    return OAuth2Client(
      clientId = OAuth2Constants.CLI_CLIENT_ID,
      name = "Tolgee CLI",
      redirectUris = properties.cliRedirectUris,
    )
  }
}
