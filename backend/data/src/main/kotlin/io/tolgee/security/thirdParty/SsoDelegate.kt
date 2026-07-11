package io.tolgee.security.thirdParty

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.enums.ThirdPartyAuthType

interface SsoDelegate : ThirdPartyAuthDelegate {
  override val name: String
    get() = "sso"

  /**
   * Also handles ThirdPartyAuthType.SSO_GLOBAL
   */
  override val preferredAuthType: ThirdPartyAuthType
    get() = ThirdPartyAuthType.SSO

  fun verifyUserSsoAccountAvailable(user: UserAccountDto): Boolean
}
