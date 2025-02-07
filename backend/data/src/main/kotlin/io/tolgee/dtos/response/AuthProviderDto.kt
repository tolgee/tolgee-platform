package io.tolgee.dtos.response

import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType

data class AuthProviderDto(
  var accountType: UserAccount.AccountType? = null,
  var authType: ThirdPartyAuthType? = null,
  var ssoDomain: String? = null,
) {
  companion object {
    fun UserAccount.asAuthProviderDto(): AuthProviderDto? {
      val type = thirdPartyAuthType ?: return null

      var ssoDomain: String? = null
      if (type == ThirdPartyAuthType.SSO) {
        ssoDomain = organizationRoles.find { it.managed }!!.organization!!.ssoTenant!!.domain
      }

      return AuthProviderDto(
        accountType,
        type,
        ssoDomain,
      )
    }

    fun AuthProviderChangeRequest.asAuthProviderDto(): AuthProviderDto? {
      return AuthProviderDto(
        accountType,
        authType,
        ssoDomain,
      )
    }
  }
}
