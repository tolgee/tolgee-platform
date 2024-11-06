package io.tolgee.security.thirdParty

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.SSO_SESSION_EXPIRATION_MINUTES
import io.tolgee.security.thirdParty.data.OAuthUserDetails
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.security.SignUpService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.addMinutes
import org.springframework.stereotype.Component

@Component
class OAuthUserHandler(
  private val signUpService: SignUpService,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun findOrCreateUser(
    userResponse: OAuthUserDetails,
    invitationCode: String?,
    thirdPartyAuthType: ThirdPartyAuthType,
    accountType: UserAccount.AccountType,
  ): UserAccount {
    val tenant = userResponse.tenant

    val userAccountOptional =
      if (thirdPartyAuthType == ThirdPartyAuthType.SSO && tenant != null) {
        userAccountService.findBySsoTenantId(tenant.entity?.id, userResponse.sub!!)
      } else {
        userAccountService.findByThirdParty(thirdPartyAuthType, userResponse.sub!!)
      }

    if (userAccountOptional.isPresent && thirdPartyAuthType == ThirdPartyAuthType.SSO) {
      updateRefreshToken(userAccountOptional.get(), userResponse.refreshToken)
      updateSsoSessionExpiry(userAccountOptional.get())
    }

    return userAccountOptional.orElseGet {
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
      if (tenant?.entity != null) {
        newUserAccount.ssoTenant = tenant.entity
      }
      newUserAccount.thirdPartyAuthType = thirdPartyAuthType
      newUserAccount.ssoRefreshToken = userResponse.refreshToken
      newUserAccount.accountType = accountType
      newUserAccount.ssoSessionExpiry = currentDateProvider.date.addMinutes(SSO_SESSION_EXPIRATION_MINUTES)

      signUpService.signUp(newUserAccount, invitationCode, null)

      // grant role to user only if request is not from oauth2 delegate
      val organization = tenant?.organization
      if (organization?.id != null &&
        thirdPartyAuthType != ThirdPartyAuthType.OAUTH2 &&
        invitationCode == null
      ) {
        organizationRoleService.grantRoleToUser(
          newUserAccount,
          organization.id,
          OrganizationRoleType.MEMBER,
        )
      }

      newUserAccount
    }
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

  fun updateSsoSessionExpiry(user: UserAccount) {
    user.ssoSessionExpiry = currentDateProvider.date.addMinutes(SSO_SESSION_EXPIRATION_MINUTES)
    userAccountService.save(user)
  }

  fun updateSsoSessionExpiry(userAccountId: Long) {
    val user = userAccountService.get(userAccountId)
    user.ssoSessionExpiry = currentDateProvider.date.addMinutes(SSO_SESSION_EXPIRATION_MINUTES)
    userAccountService.save(user)
  }
}
