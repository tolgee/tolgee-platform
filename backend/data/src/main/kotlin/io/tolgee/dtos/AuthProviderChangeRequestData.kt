package io.tolgee.dtos

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import java.util.*

data class AuthProviderChangeRequestData(
  val userAccount: UserAccount,
  val newAuthProvider: ThirdPartyAuthType?,
  val oldAuthProvider: ThirdPartyAuthType?,
  val newAccountType: UserAccount.AccountType,
  val oldAccountType: UserAccount.AccountType?,
  val ssoDomain: String? = null,
  val sub: String? = null,
  val refreshToken: String? = null,
  val calculateExpirationDate: Date? = null,
)
