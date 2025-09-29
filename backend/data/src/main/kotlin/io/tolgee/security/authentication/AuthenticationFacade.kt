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
import io.tolgee.dtos.cacheable.ApiKeyDto
import io.tolgee.dtos.cacheable.PatDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.queryResults.UserAccountView
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.ApiKey
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.PatService
import io.tolgee.service.security.UserAccountService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticationFacade(
  private val userAccountService: UserAccountService,
  private val apiKeyService: ApiKeyService,
  private val patService: PatService,
) {
  // -- GENERAL AUTHENTICATION INFO
  val isAuthenticated: Boolean
    get() = SecurityContextHolder.getContext().authentication is TolgeeAuthentication

  val authentication: TolgeeAuthentication
    get() =
      SecurityContextHolder.getContext().authentication as? TolgeeAuthentication
        ?: throw AuthenticationException(Message.UNAUTHENTICATED)

  // -- CURRENT USER
  val authenticatedUser: UserAccountDto
    get() = authentication.principal

  val authenticatedUserOrNull: UserAccountDto?
    get() = if (isAuthenticated) authentication.principal else null

  val authenticatedUserEntity: UserAccount
    get() = authenticatedUserEntityOrNull ?: throw AuthenticationException(Message.UNAUTHENTICATED)

  val authenticatedUserEntityOrNull: UserAccount?
    get() =
      authenticatedUserOrNull?.let {
        if (authentication.userAccountEntity == null) {
          authentication.userAccountEntity = userAccountService.findActive(it.id)
        }

        return authentication.userAccountEntity
      }

  val authenticatedUserView: UserAccountView
    get() = authenticatedUserViewOrNull ?: throw AuthenticationException(Message.UNAUTHENTICATED)

  val authenticatedUserViewOrNull: UserAccountView?
    get() =
      authenticatedUserOrNull?.let {
        if (authentication.userAccountView == null) {
          authentication.userAccountView = userAccountService.findActiveView(it.id)
        }

        return authentication.userAccountView
      }

  // -- ACTING USER
  val actingUser: UserAccountDto?
    get() = authentication.actingAsUserAccount

  // -- AUTHENTICATION METHOD AND DETAILS
  val deviceId: String?
    get() = authentication.deviceId
  val isReadOnly: Boolean
    get() = authentication.isReadOnly

  val isUserSuperAuthenticated: Boolean
    get() = if (isAuthenticated) authentication.isSuperToken else false

  val isApiAuthentication: Boolean
    get() = isProjectApiKeyAuth || isPersonalAccessTokenAuth

  val isProjectApiKeyAuth: Boolean
    get() = if (isAuthenticated) authentication.credentials is ApiKeyDto else false

  val isPersonalAccessTokenAuth: Boolean
    get() = if (isAuthenticated) authentication.credentials is PatDto else false

  val projectApiKey: ApiKeyDto
    get() = authentication.credentials as ApiKeyDto

  val projectApiKeyEntity: ApiKey
    get() {
      if (authentication.projectApiKeyEntity == null) {
        authentication.projectApiKeyEntity = apiKeyService.get(projectApiKey.id)
      }

      // null safety: `.get` returns non-null or throws. non-null assert is safe here.
      return authentication.projectApiKeyEntity!!
    }

  val personalAccessToken: PatDto
    get() = authentication.credentials as PatDto

  val personalAccessTokenEntity: Pat
    get() {
      if (authentication.personalAccessTokenEntity == null) {
        authentication.personalAccessTokenEntity = patService.get(personalAccessToken.id)
      }

      // null safety: `.get` returns non-null or throws. non-null assert is safe here.
      return authentication.personalAccessTokenEntity!!
    }
}
