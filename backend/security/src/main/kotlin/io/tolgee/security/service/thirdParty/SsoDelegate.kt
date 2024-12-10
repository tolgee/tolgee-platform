package io.tolgee.security.service.thirdParty

import io.tolgee.dtos.cacheable.UserAccountDto

interface SsoDelegate : ThirdPartyAuthDelegate {
  override val name: String
    get() = "sso"

  fun verifyUserSsoAccountAvailable(user: UserAccountDto): Boolean
}
