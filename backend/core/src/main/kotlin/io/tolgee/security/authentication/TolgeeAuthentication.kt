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

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.queryResults.UserAccountView
import io.tolgee.model.ApiKey
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class TolgeeAuthentication(
  private val credentials: Any?,
  /**
   * Device id - unique for each token. For activity logging.
   */
  val deviceId: String?,
  private val userAccount: UserAccountDto,
  /**
   * If this is an impersonation token, this property will contain
   * the user account, which initiated the impersonation.
   */
  val actingAsUserAccount: UserAccountDto?,
  /**
   * Whether the token can be used only for read-only requests.
   */
  val isReadOnly: Boolean,
  /**
   * Whether the user is super-authenticated
   */
  val isSuperToken: Boolean = false,
  private val details: TolgeeAuthenticationDetails? = null,
) : Authentication {
  var userAccountEntity: UserAccount? = null
  var userAccountView: UserAccountView? = null
  var projectApiKeyEntity: ApiKey? = null
  var personalAccessTokenEntity: Pat? = null

  override fun getName(): String {
    return userAccount.username
  }

  override fun getAuthorities(): Collection<GrantedAuthority> {
    return when (userAccount.role) {
      UserAccount.Role.USER ->
        listOf(
          SimpleGrantedAuthority(ROLE_USER),
        )

      UserAccount.Role.SUPPORTER ->
        listOf(
          SimpleGrantedAuthority(ROLE_USER),
          SimpleGrantedAuthority(ROLE_SUPPORTER),
        )

      UserAccount.Role.ADMIN ->
        listOf(
          SimpleGrantedAuthority(ROLE_USER),
          SimpleGrantedAuthority(ROLE_SUPPORTER),
          SimpleGrantedAuthority(ROLE_ADMIN),
        )

      null -> emptyList()
    } + authorityFromIsReadOnly
  }

  private val authorityFromIsReadOnly: GrantedAuthority
    get() {
      return SimpleGrantedAuthority(
        if (isReadOnly) {
          ROLE_READ_ONLY
        } else {
          ROLE_READ_WRITE
        },
      )
    }

  override fun getCredentials(): Any? {
    return credentials
  }

  override fun getDetails(): TolgeeAuthenticationDetails? {
    return details
  }

  override fun getPrincipal(): UserAccountDto {
    return userAccount
  }

  override fun isAuthenticated(): Boolean {
    return true
  }

  override fun setAuthenticated(isAuthenticated: Boolean) {
    throw IllegalArgumentException("Implementation is immutable")
  }

  companion object {
    const val ROLE_USER = "ROLE_USER"
    const val ROLE_SUPPORTER = "ROLE_SUPPORTER"
    const val ROLE_ADMIN = "ROLE_ADMIN"
    const val ROLE_READ_ONLY = "ROLE_READ_ONLY"
    const val ROLE_READ_WRITE = "ROLE_READ_WRITE"
  }
}
