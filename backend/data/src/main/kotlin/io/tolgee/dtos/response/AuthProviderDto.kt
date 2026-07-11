package io.tolgee.dtos.response

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType

data class AuthProviderDto(
  var id: String? = null,
  var authType: ThirdPartyAuthType? = null,
  var ssoDomain: String? = null,
) {
  companion object {
    fun UserAccount.asAuthProviderDto(properties: TolgeeProperties): AuthProviderDto? {
      val type = thirdPartyAuthType ?: return null

      var ssoDomain: String? =
        when (type) {
          ThirdPartyAuthType.SSO ->
            organizationRoles
              .find { it.managed }!!
              .organization!!
              .ssoTenant!!
              .domain

          ThirdPartyAuthType.SSO_GLOBAL -> properties.authentication.ssoGlobal.domain
          else -> null
        }

      return AuthProviderDto(
        null,
        type,
        ssoDomain,
      )
    }

    fun AuthProviderChangeRequest.asAuthProviderDto(): AuthProviderDto {
      return AuthProviderDto(
        identifier,
        authType,
        ssoDomain,
      )
    }
  }
}
