package io.tolgee.dtos.request.auth

import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import java.util.Date

data class AuthProviderChangeData(
  var userAccount: UserAccount,
  var accountType: UserAccount.AccountType,
  var authType: ThirdPartyAuthType,
  var authId: String? = null,
  var ssoDomain: String? = null,
  var ssoRefreshToken: String? = null,
  var ssoExpiration: Date? = null,
) {
  fun asAuthProviderChangeRequest(): AuthProviderChangeRequest {
    return AuthProviderChangeRequest().also {
      it.userAccount = this.userAccount
      it.authType = this.authType
      it.authId = this.authId
      it.ssoRefreshToken = this.ssoRefreshToken
      it.ssoExpiration = this.ssoExpiration
      it.ssoDomain = this.ssoDomain
      it.ssoRefreshToken = this.ssoRefreshToken
      it.ssoExpiration = this.ssoExpiration
    }
  }
}
