package io.tolgee.dtos.request.auth

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
)
