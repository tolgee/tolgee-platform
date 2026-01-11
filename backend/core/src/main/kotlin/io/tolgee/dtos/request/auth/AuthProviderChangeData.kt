package io.tolgee.dtos.request.auth

import io.tolgee.model.enums.ThirdPartyAuthType
import java.util.Date

data class AuthProviderChangeData(
  var authType: ThirdPartyAuthType?,
  var authId: String? = null,
  var ssoDomain: String? = null,
  var ssoRefreshToken: String? = null,
  var ssoExpiration: Date? = null,
)
