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
import io.tolgee.security.oauth2.OAuth2TokenCredentials
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
    get() = isProjectApiKeyAuth || isPersonalAccessTokenAuth || isOAuthTokenAuth

  val isProjectApiKeyAuth: Boolean
    get() = isProjectApiKeyCredential()

  val isPersonalAccessTokenAuth: Boolean
    get() = isPersonalAccessTokenCredential()

  val isOAuthTokenAuth: Boolean
    get() = oauthTokenCredentials != null

  val oauthTokenCredentials: OAuth2TokenCredentials?
    get() = currentOAuthTokenCredentials()

  val isScopedCredential: Boolean
    get() = isProjectApiKeyAuth || isOAuthTokenAuth

  /** [isScopedCredentialInContextFor] is the same predicate for callers that cannot take this bean - see
   * PermissionService, which this class's own dependencies would make cyclic. */
  fun isScopedCredentialFor(userAccountId: Long): Boolean = isScopedCredentialInContextFor(userAccountId)

  /** An elevation granted to the user, so a scoped credential must carry the real scope instead. */
  val canUseAuthorSelfAccess: Boolean
    get() = !isScopedCredential

  fun isAuthorSelfAccess(authorId: Long?): Boolean {
    if (!canUseAuthorSelfAccess) return false
    return authorId != null && authorId == authenticatedUser.id
  }

  val implicitProjectId: Long?
    get() {
      if (isProjectApiKeyAuth) return projectApiKey.projectId
      return oauthTokenCredentials?.singleProjectId()
    }

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
