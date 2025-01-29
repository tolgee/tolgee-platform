package io.tolgee.dtos.response

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
)
