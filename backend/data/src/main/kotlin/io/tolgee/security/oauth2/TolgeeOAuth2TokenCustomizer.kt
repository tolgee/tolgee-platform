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

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.stereotype.Component

/**
 * Adds Tolgee's `tg.prj` (project set) and `aud` claims to OAuth2 access tokens. `sub` is SAS's default — the
 * authorization principal's name, which the session bootstrap sets to the numeric user id.
 */
@Component
class TolgeeOAuth2TokenCustomizer(
  private val audienceResolver: OAuth2AudienceResolver,
) : OAuth2TokenCustomizer<JwtEncodingContext> {
  override fun customize(context: JwtEncodingContext) {
    if (context.tokenType != OAuth2TokenType.ACCESS_TOKEN) return

    context.claims.claim(OAuth2Constants.PROJECTS_CLAIM, projectSet(context))
    context.claims.audience(listOf(audienceResolver.apiAudience))
  }

  private fun projectSet(context: JwtEncodingContext): Any {
    // Stamp ids as strings: SAS's JDBC polymorphic-type validator rejects java.lang.Long when it deserializes the
    // stored claims on the refresh grant, which would otherwise make a project-bound token unrefreshable.
    projectHint(context)?.let { return listOf(it.toString()) }
    return OAuth2Constants.ALL_PROJECTS
  }

  private fun projectHint(context: JwtEncodingContext): Long? {
    val authorizationRequest =
      context.getAuthorization()?.getAttribute<OAuth2AuthorizationRequest>(OAuth2AuthorizationRequest::class.java.name)
    val raw = authorizationRequest?.additionalParameters?.get(OAuth2Constants.PROJECT_PARAM) as? String
    return raw?.toLongOrNull()
  }
}
