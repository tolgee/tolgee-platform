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
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.ApiKey
import io.tolgee.model.Pat
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticationFacade {
  internal var _authenticatedProject: Project? = null

  val authentication: TolgeeAuthentication
    get() = SecurityContextHolder.getContext().authentication as? TolgeeAuthentication
      ?: throw AuthenticationException(Message.UNAUTHENTICATED)

  val authenticatedUser: UserAccount
    get() = authentication.principal

  val authenticatedProject: Project
    get() = _authenticatedProject
      ?: throw IllegalStateException("No authenticated project.")

  val isUserSuperAuthenticated: Boolean
    get() = authentication.details?.isSuperToken == true

  val isApiAuthentication: Boolean
    get() = authentication.credentials is ApiKey || authentication.credentials is Pat

  val isProjectApiKeyAuth: Boolean
    get() = authentication.credentials is ApiKey

  val isPersonalAccessTokenAuth: Boolean
    get() = authentication.credentials is Pat

  val projectApiKey: ApiKey
    get() = authentication.credentials as ApiKey

  val personalAccessToken: Pat
    get() = authentication.credentials as Pat
}
