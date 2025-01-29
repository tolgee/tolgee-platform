package io.tolgee.dtos.response

import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import java.util.Date

data class AuthProviderDto(
  var accountType: UserAccount.AccountType? = null,
  var authType: ThirdPartyAuthType? = null,
  var authId: String? = null,
  var ssoDomain: String? = null,
  var ssoRefreshToken: String? = null,
  var ssoExpiration: Date? = null,
) {
  companion object {
    fun UserAccount.asAuthProviderDto(): AuthProviderDto? {
      val type = thirdPartyAuthType ?: return null

      var ssoDomain: String?
      var ssoExpiration: Date?
      var ssoRefreshToken: String?
      when (type) {
        ThirdPartyAuthType.SSO -> {
          ssoDomain = organizationRoles.find { it.managed }!!.organization!!.ssoTenant!!.domain
          ssoExpiration = ssoSessionExpiry
          ssoRefreshToken = this.ssoRefreshToken
        }

        ThirdPartyAuthType.SSO_GLOBAL -> {
          ssoDomain = null
          ssoExpiration = ssoSessionExpiry
          ssoRefreshToken = this.ssoRefreshToken
        }

        else -> {
          ssoDomain = null
          ssoExpiration = null
          ssoRefreshToken = null
        }
      }

      return AuthProviderDto(
        accountType,
        type,
        thirdPartyAuthId,
        ssoDomain = ssoDomain,
        ssoExpiration = ssoExpiration,
        ssoRefreshToken = ssoRefreshToken,
      )
    }

    fun AuthProviderChangeRequest.asAuthProviderDto(): AuthProviderDto? {
      return AuthProviderDto(
        accountType,
        authType,
        authId,
        ssoDomain,
        ssoRefreshToken,
        ssoExpiration,
      )
    }
  }
}
