package io.tolgee.security.thirdParty

import io.tolgee.component.cacheWithExpiration.CacheWithExpirationManager
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.thirdParty.data.OAuthUserDetails
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.security.SignUpService
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Component

@Component
class OAuthUserHandler(
  private val signUpService: SignUpService,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
  private val cacheWithExpirationManager: CacheWithExpirationManager,
) {
  fun findOrCreateUser(
    userResponse: OAuthUserDetails,
    invitationCode: String?,
    thirdPartyAuthType: ThirdPartyAuthType,
  ): UserAccount {
    val tenant = userResponse.tenant

    val userAccountOptional =
      if (thirdPartyAuthType == ThirdPartyAuthType.SSO && tenant?.domain != null) {
        userAccountService.findByDomainSso(tenant.domain, userResponse.sub!!)
      } else {
        userAccountService.findByThirdParty(thirdPartyAuthType, userResponse.sub!!)
      }

    if (userAccountOptional.isPresent && thirdPartyAuthType == ThirdPartyAuthType.SSO) {
      updateRefreshToken(userAccountOptional.get(), userResponse.refreshToken)
      cacheSsoUser(userAccountOptional.get().id, thirdPartyAuthType)
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
      if (tenant?.domain != null) {
        newUserAccount.ssoTenant = tenant
      }
      newUserAccount.thirdPartyAuthType = thirdPartyAuthType
      newUserAccount.ssoRefreshToken = userResponse.refreshToken
      newUserAccount.accountType = UserAccount.AccountType.THIRD_PARTY

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

      cacheSsoUser(newUserAccount.id, thirdPartyAuthType)

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

    if (userAccount.ssoRefreshToken != refreshToken) {
      userAccount.ssoRefreshToken = refreshToken
      userAccountService.save(userAccount)
    }
  }

  private fun cacheSsoUser(
    userId: Long,
    thirdPartyAuthType: ThirdPartyAuthType,
  ) {
    if (thirdPartyAuthType == ThirdPartyAuthType.SSO) {
      cacheWithExpirationManager.putCache(Caches.IS_SSO_USER_VALID, userId, true)
    }
  }
}
