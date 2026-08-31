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

package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.ApiKeyDto
import io.tolgee.dtos.cacheable.PatDto
import io.tolgee.security.oauth2.OAuth2TokenCredentials
import org.springframework.security.core.context.SecurityContextHolder

fun isScopedCredentialInContextFor(userAccountId: Long): Boolean =
  isScopedCredentialInContext() && currentAuthentication()?.principal?.id == userAccountId

internal fun isScopedCredentialInContext(): Boolean =
  isProjectApiKeyCredential() || currentOAuthTokenCredentials() != null

internal fun isProjectApiKeyCredential(): Boolean = currentAuthentication()?.credentials is ApiKeyDto

internal fun isPersonalAccessTokenCredential(): Boolean = currentAuthentication()?.credentials is PatDto

internal fun currentOAuthTokenCredentials(): OAuth2TokenCredentials? =
  currentAuthentication()?.credentials as? OAuth2TokenCredentials

private fun currentAuthentication(): TolgeeAuthentication? =
  SecurityContextHolder.getContext().authentication as? TolgeeAuthentication
