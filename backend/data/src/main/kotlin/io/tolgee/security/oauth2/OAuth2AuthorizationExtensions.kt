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
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization

fun OAuth2Authorization.projectHint(): Long? {
  val request = authorizationRequest() ?: return null
  return (request.additionalParameters[OAuth2Constants.PROJECT_PARAM] as? String)?.toLongOrNull()
}

/** Scopes the client asked for, before the user narrowed them on the consent screen. */
fun OAuth2Authorization.authorizationRequestScopes(): Set<String>? = authorizationRequest()?.scopes

private fun OAuth2Authorization.authorizationRequest(): OAuth2AuthorizationRequest? =
  getAttribute(OAuth2AuthorizationRequest::class.java.name)
