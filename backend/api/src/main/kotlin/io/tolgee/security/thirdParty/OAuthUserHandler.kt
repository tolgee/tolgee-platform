package io.tolgee.security.thirdParty

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.SsoGlobalProperties
import io.tolgee.configuration.tolgee.SsoLocalProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.thirdParty.data.OAuthUserDetails
import io.tolgee.service.AuthProviderChangeRequestService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.security.SignUpService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.addMinutes
import org.springframework.stereotype.Component
import java.util.Date

@Component
class OAuthUserHandler(
  private val signUpService: SignUpService,
  private val organizationRoleService: OrganizationRoleService,
  private val ssoLocalProperties: SsoLocalProperties,
  private val ssoGlobalProperties: SsoGlobalProperties,
  private val userAccountService: UserAccountService,
  private val currentDateProvider: CurrentDateProvider,
  private val authenticationProperties: AuthenticationProperties,
  private val authProviderChangeRequestService: AuthProviderChangeRequestService,
) {
  fun findOrCreateUser(
    userResponse: OAuthUserDetails,
    invitationCode: String?,
    thirdPartyAuthType: ThirdPartyAuthType,
    accountType: UserAccount.AccountType,
  ): UserAccount {
    val userAccountOptional =
      if (thirdPartyAuthType == ThirdPartyAuthType.SSO) {
        if (userResponse.tenant == null) {
          // This should never happen
          throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)
        }
        userAccountService.findBySsoDomain(userResponse.tenant.domain, userResponse.sub!!)
      } else {
        // SSO_GLOBAL or OAUTH2
        userAccountService.findByThirdParty(thirdPartyAuthType, userResponse.sub!!)
      }

    userAccountOptional.ifPresent {
      if (
        thirdPartyAuthType == ThirdPartyAuthType.SSO ||
        thirdPartyAuthType == ThirdPartyAuthType.SSO_GLOBAL
      ) {
        updateRefreshToken(it, userResponse.refreshToken)
        resetSsoSessionExpiry(it)
      }
    }

    return userAccountOptional.orElseGet {
      userAccountService.findActive(userResponse.email)?.let {
        return@orElseGet manageUserNameConflict(it, userResponse, thirdPartyAuthType)
      }
      createUser(userResponse, invitationCode, thirdPartyAuthType, accountType)
    }
  }

  private fun createUser(
    userResponse: OAuthUserDetails,
    invitationCode: String?,
    thirdPartyAuthType: ThirdPartyAuthType,
    accountType: UserAccount.AccountType,
  ): UserAccount {
    userAccountService.findActive(userResponse.email)?.let {
      throw AuthenticationException(Message.USERNAME_ALREADY_EXISTS)
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
    newUserAccount.ssoSessionExpiry = ssoCurrentExpiration(thirdPartyAuthType)

    val organization = userResponse.tenant?.organization
    signUpService.signUp(newUserAccount, invitationCode, null, organizationForced = organization)

    if (organization != null) {
      organizationRoleService.setManaged(newUserAccount, organization, true)
    }

    return newUserAccount
  }

  private fun manageUserNameConflict(
    user: UserAccount,
    userResponse: OAuthUserDetails,
    thirdPartyAuthType: ThirdPartyAuthType
  ): UserAccount {
    val request = authProviderChangeRequestService.create(
      user,
      thirdPartyAuthType,
      user.thirdPartyAuthType,
      UserAccount.AccountType.THIRD_PARTY,
      user.accountType,
      userResponse.tenant?.domain,
      userResponse.sub,
      userResponse.refreshToken,
      calculateExpirationDate()
    )

    throw AuthenticationException(Message.USERNAME_ALREADY_EXISTS, listOf(request.id))
  }

  private fun changeAuthProvider(
    user: UserAccount,
    thirdPartyAuthType: ThirdPartyAuthType,
    userDetails: OAuthUserDetails,
  ): UserAccount {
    user.thirdPartyAuthType = thirdPartyAuthType
    user.accountType = UserAccount.AccountType.THIRD_PARTY
    user.thirdPartyAuthId = userDetails.sub
    user.ssoTenant = userDetails.tenant
    if (thirdPartyAuthType == ThirdPartyAuthType.SSO) {
      updateRefreshToken(user, userDetails.refreshToken)
      updateSsoSessionExpiry(user)
    }
    return userAccountService.save(user)
  }

  fun updateRefreshToken(
    userAccount: UserAccount,
    refreshToken: String?,
  ) {
    if (userAccount.ssoRefreshToken != refreshToken) {
      userAccount.ssoRefreshToken = refreshToken
      userAccountService.save(userAccount)
    }
  }

  fun updateRefreshToken(
    userAccountId: Long,
    refreshToken: String?,
  ) {
    val userAccount = userAccountService.get(userAccountId)
    updateRefreshToken(userAccount, refreshToken)
  }

  fun resetSsoSessionExpiry(user: UserAccount) {
    user.ssoSessionExpiry = ssoCurrentExpiration(user.thirdPartyAuthType)
    userAccountService.save(user)
  }

  fun resetSsoSessionExpiry(userAccountId: Long) {
    val user = userAccountService.get(userAccountId)
    resetSsoSessionExpiry(user)
  }

  private fun ssoCurrentExpiration(type: ThirdPartyAuthType?): Date? {
    return currentDateProvider.date.addMinutes(
      when (type) {
        ThirdPartyAuthType.SSO -> ssoLocalProperties.sessionExpirationMinutes
        ThirdPartyAuthType.SSO_GLOBAL -> ssoGlobalProperties.sessionExpirationMinutes
        else -> return null
      },
    )
  }
}
