/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
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

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.ApiKey
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import io.tolgee.service.security.UserAccountService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticationFacade(
  private val userAccountService: UserAccountService
) {
  val isAuthenticated: Boolean
    get() = SecurityContextHolder.getContext().authentication is TolgeeAuthentication

  val authentication: TolgeeAuthentication
    get() = SecurityContextHolder.getContext().authentication as? TolgeeAuthentication
      ?: throw AuthenticationException(Message.UNAUTHENTICATED)

  val authenticatedUser: UserAccountDto
    get() = authentication.principal

  val authenticatedUserOrNull: UserAccountDto?
    get() = if (isAuthenticated) authentication.principal else null

  val authenticatedUserEntity: UserAccount
    get() = authenticatedUserEntityOrNull ?: throw AuthenticationException(Message.UNAUTHENTICATED)

  val authenticatedUserEntityOrNull: UserAccount?
    get() = if (isAuthenticated) userAccountService.findActive(authenticatedUser.id) else null

  val isUserSuperAuthenticated: Boolean
    get() = if (isAuthenticated) authentication.details?.isSuperToken == true else false

  val isApiAuthentication: Boolean
    get() = if (isAuthenticated) authentication.credentials is ApiKey || authentication.credentials is Pat else false

  val isProjectApiKeyAuth: Boolean
    get() = if (isAuthenticated) authentication.credentials is ApiKey else false

  val isPersonalAccessTokenAuth: Boolean
    get() = if (isAuthenticated) authentication.credentials is Pat else false

  val projectApiKey: ApiKey
    get() = authentication.credentials as ApiKey

  val personalAccessToken: Pat
    get() = authentication.credentials as Pat
}
