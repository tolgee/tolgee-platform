package io.tolgee.security.thirdParty.data

import io.tolgee.constants.Message
import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.thirdParty.GithubOAuthDelegate.GithubUserResponse
import io.tolgee.security.thirdParty.GoogleOAuthDelegate.GoogleUserResponse

data class ThirdPartyUserDetails(
  val authId: String,
  val username: String,
  val name: String,
  val thirdPartyAuthType: ThirdPartyAuthType,
  val invitationCode: String? = null,
  val refreshToken: String? = null,
  val tenant: SsoTenantConfig? = null,
) {
  companion object {
    fun fromOAuth2(
      data: OAuthUserDetails,
      authType: ThirdPartyAuthType,
      invitationCode: String?,
    ): ThirdPartyUserDetails {
      return ThirdPartyUserDetails(
        authId = data.sub,
        username = data.email,
        name =
          data.name ?: run {
            if (data.givenName != null && data.familyName != null) {
              "${data.givenName} ${data.familyName}"
            } else {
              data.email.split("@")[0]
            }
          },
        thirdPartyAuthType = authType,
        invitationCode = invitationCode,
        refreshToken = data.refreshToken,
        tenant = data.tenant,
      )
    }

    fun fromGithub(
      data: GithubUserResponse,
      email: String,
      invitationCode: String?,
    ): ThirdPartyUserDetails {
      return ThirdPartyUserDetails(
        authId = data.id!!,
        username = email,
        name = data.name ?: data.login,
        thirdPartyAuthType = ThirdPartyAuthType.GITHUB,
        invitationCode = invitationCode,
      )
    }

    fun fromGoogle(
      data: GoogleUserResponse,
      invitationCode: String?,
    ): ThirdPartyUserDetails {
      return ThirdPartyUserDetails(
        authId = data.sub!!,
        username = data.email ?: throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL),
        name = data.name ?: (data.given_name + " " + data.family_name),
        thirdPartyAuthType = ThirdPartyAuthType.GOOGLE,
        invitationCode = invitationCode,
      )
    }
  }
}
