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

import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService

/**
 * Remembers no consent, so the consent screen runs on every authorization.
 *
 * Skipping it would leave the authorization with no project selection, which [TolgeeOAuth2TokenCustomizer] refuses to
 * mint a token for. See "Consent is never remembered" in docs/oauth/README.md for why the two are tied together.
 */
class AlwaysPromptConsentService : OAuth2AuthorizationConsentService {
  override fun save(authorizationConsent: OAuth2AuthorizationConsent) = Unit

  override fun remove(authorizationConsent: OAuth2AuthorizationConsent) = Unit

  override fun findById(
    registeredClientId: String,
    principalName: String,
  ): OAuth2AuthorizationConsent? = null
}
