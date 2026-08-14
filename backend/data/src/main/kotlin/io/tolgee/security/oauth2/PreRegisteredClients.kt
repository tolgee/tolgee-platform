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
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.stereotype.Component

/** Seeds the first-party public clients (browser extension, CLI). Idempotent: row id = client id, so re-runs update in place. (MCP clients self-register via CIMD.) */
@Component
class PreRegisteredClients(
  private val registeredClientRepository: RegisteredClientRepository,
  private val properties: OAuth2ServerProperties,
) : ApplicationRunner {
  override fun run(args: ApplicationArguments) {
    browserExtensionClient()?.let { registeredClientRepository.save(it) }
    cliClient()?.let { registeredClientRepository.save(it) }
  }

  private fun browserExtensionClient(): RegisteredClient? {
    if (properties.browserExtensionRedirectUris.isEmpty()) return null
    return publicClientBuilder(OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID, "Tolgee Browser Extension")
      .apply { properties.browserExtensionRedirectUris.forEach { redirectUri(it) } }
      .clientSettings(
        clientSettings(
          requireConsent = true,
          requiredScopes = listOf(Scope.KEYS_VIEW, Scope.TRANSLATIONS_VIEW),
        ),
      ).build()
  }

  private fun cliClient(): RegisteredClient? {
    // A client with no redirect URIs cannot complete the authorization_code flow, so skip it (as the extension does).
    if (properties.cliRedirectUris.isEmpty()) return null
    return publicClientBuilder(OAuth2Constants.CLI_CLIENT_ID, "Tolgee CLI")
      .apply { properties.cliRedirectUris.forEach { redirectUri(it) } }
      // A loopback redirect can't be bound to one local app, so any local process that knows this fixed client_id could
      // otherwise obtain a full-scope token silently. Require consent (OAuth 2.1 / RFC 8252 for public native clients).
      .clientSettings(clientSettings(requireConsent = true))
      .build()
  }

  private fun publicClientBuilder(
    clientId: String,
    clientName: String,
  ): RegisteredClient.Builder {
    return RegisteredClient
      .withId(clientId)
      .clientId(clientId)
      .clientName(clientName)
      .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
      .apply { Scope.entries.forEach { scope(it.value) } }
      .tokenSettings(properties.tokenSettings())
  }

  private fun clientSettings(
    requireConsent: Boolean,
    requiredScopes: List<Scope> = emptyList(),
  ): ClientSettings {
    val builder =
      ClientSettings
        .builder()
        .requireProofKey(true)
        .requireAuthorizationConsent(requireConsent)
    if (requiredScopes.isNotEmpty()) {
      builder.setting(OAuth2Constants.REQUIRED_SCOPES_SETTING, requiredScopes.joinToString(" ") { it.value })
    }
    return builder.build()
  }
}
