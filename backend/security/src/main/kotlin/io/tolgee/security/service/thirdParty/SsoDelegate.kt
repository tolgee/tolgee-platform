package io.tolgee.security.service.thirdParty

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType

interface SsoDelegate : ThirdPartyAuthDelegate {
  override val name: String
    get() = "sso"

  override val preferredAccountType: UserAccount.AccountType
    get() = UserAccount.AccountType.MANAGED

  /**
   * Also handles ThirdPartyAuthType.SSO_GLOBAL
   */
  override val preferredThirdPartyAuthType: ThirdPartyAuthType
    get() = ThirdPartyAuthType.SSO

  fun verifyUserSsoAccountAvailable(user: UserAccountDto): Boolean
}
