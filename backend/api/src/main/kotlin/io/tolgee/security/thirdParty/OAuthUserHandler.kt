package io.tolgee.security.thirdParty

import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.AuthProviderChangeData
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.thirdParty.data.OAuthUserDetails
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.security.AuthProviderChangeService
import io.tolgee.service.security.SignUpService
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Component

@Component
class OAuthUserHandler(
  private val signUpService: SignUpService,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
  private val authProviderChangeService: AuthProviderChangeService,
) {
  fun findOrCreateUser(
    userResponse: OAuthUserDetails,
    invitationCode: String?,
    thirdPartyAuthType: ThirdPartyAuthType,
    accountType: UserAccount.AccountType,
  ): UserAccount {
    val userAccount =
      getUserAccount(thirdPartyAuthType, userResponse)

    if (userAccount != null) {
      if (thirdPartyAuthType in arrayOf(ThirdPartyAuthType.SSO, ThirdPartyAuthType.SSO_GLOBAL)) {
        userAccountService.updateSsoSession(userAccount, userResponse.refreshToken)
      }
      return userAccount
    }

    return createUser(userResponse, invitationCode, thirdPartyAuthType, accountType)
  }

  private fun getUserAccount(
    thirdPartyAuthType: ThirdPartyAuthType,
    userResponse: OAuthUserDetails,
  ): UserAccount? {
    if (thirdPartyAuthType == ThirdPartyAuthType.SSO) {
      if (userResponse.tenant == null) {
        // This should never happen
        throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)
      }
      return userAccountService.findEnabledBySsoDomain(userResponse.tenant.domain, userResponse.sub!!)
    }
    if (thirdPartyAuthType !in arrayOf(ThirdPartyAuthType.SSO_GLOBAL, ThirdPartyAuthType.OAUTH2)) {
      // This should never happen
      throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)
    }
    return userAccountService.findByThirdParty(thirdPartyAuthType, userResponse.sub!!)
  }

  private fun createUser(
    userResponse: OAuthUserDetails,
    invitationCode: String?,
    thirdPartyAuthType: ThirdPartyAuthType,
    accountType: UserAccount.AccountType,
  ): UserAccount {
    val existingUserAccount =
      userAccountService.findActive(userResponse.email)
    if (existingUserAccount != null) {
      authProviderChangeService.initiateProviderChange(
        AuthProviderChangeData(
          existingUserAccount,
          accountType,
          thirdPartyAuthType,
          userResponse.sub,
          ssoDomain = userResponse.tenant?.domain,
          ssoRefreshToken = userResponse.refreshToken,
          ssoExpiration = userAccountService.getCurrentSsoExpiration(thirdPartyAuthType),
        ),
      )
    }

    val newUserAccount = UserAccount()
    newUserAccount.username = userResponse.email

    val name =
      userResponse.name ?: run {
        if (userResponse.givenName != null && userResponse.familyName != null) {
          "${userResponse.givenName} ${userResponse.familyName}"
        } else {
          userResponse.email.split("@")[0]
        }
      }
    newUserAccount.name = name
    newUserAccount.thirdPartyAuthId = userResponse.sub
    newUserAccount.thirdPartyAuthType = thirdPartyAuthType
    newUserAccount.ssoRefreshToken = userResponse.refreshToken
    newUserAccount.accountType = accountType
    newUserAccount.ssoSessionExpiry = userAccountService.getCurrentSsoExpiration(thirdPartyAuthType)

    val organization = userResponse.tenant?.organization
    signUpService.signUp(newUserAccount, invitationCode, null, organizationForced = organization)

    if (organization != null) {
      organizationRoleService.setManaged(newUserAccount, organization, true)
    }

    return newUserAccount
  }
}
