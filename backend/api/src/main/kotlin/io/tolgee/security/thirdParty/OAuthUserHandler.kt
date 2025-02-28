package io.tolgee.security.thirdParty

import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.AuthProviderChangeData
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.thirdParty.data.OAuthUserDetails
import io.tolgee.service.TenantService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.AuthProviderChangeService
import io.tolgee.service.security.SignUpService
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Component

@Component
class OAuthUserHandler(
  private val signUpService: SignUpService,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
  private val authProviderChangeService: AuthProviderChangeService,
  private val tenantService: TenantService,
) {
  fun findOrCreateUser(
    userResponse: OAuthUserDetails,
    invitationCode: String?,
    thirdPartyAuthType: ThirdPartyAuthType,
    accountType: UserAccount.AccountType,
  ): UserAccount {
    val userAccount =
      getUserAccount(thirdPartyAuthType, userResponse)

    authProviderChangeService.tryInitiate(
      userAccount,
      userResponse.email,
      AuthProviderChangeData(
        accountType,
        thirdPartyAuthType,
        userResponse.sub,
        ssoDomain = userResponse.tenant?.domain,
        ssoRefreshToken = userResponse.refreshToken,
        ssoExpiration = userAccountService.getCurrentSsoExpiration(thirdPartyAuthType),
      ),
    )

    if (userAccount != null) {
      if (thirdPartyAuthType in arrayOf(ThirdPartyAuthType.SSO, ThirdPartyAuthType.SSO_GLOBAL)) {
        userAccountService.updateSsoSession(userAccount, userResponse.refreshToken)
      }
      return userAccount
    }

    return createUser(userResponse, invitationCode, thirdPartyAuthType, accountType)
  }

  private fun getManagingOrganization(userResponse: OAuthUserDetails): Organization? {
    return userResponse.tenant?.organizationId?.let {
      organizationService.find(it) ?: throw AuthenticationException(Message.ORGANIZATION_NOT_FOUND)
    }
  }

  private fun checkNotManagedByOrganization(domain: String?) {
    if (tenantService.getEnabledConfigByDomainOrNull(domain) != null) {
      // There is sso configured for the domain - don't allow sign up without sso
      throw AuthenticationException(Message.USE_SSO_FOR_AUTHENTICATION_INSTEAD, listOf(domain))
    }
  }

  private fun checkUserManagingOrganization(
    userResponse: OAuthUserDetails,
    user: UserAccount,
  ) {
    val org = getManagingOrganization(userResponse)
    if (org == null) {
      checkNotManagedByOrganization(user.domain)
      return
    }

    if (user.organizationRoles.any { it.organization?.id != org.id }) {
      // User accepted invitation for different organization
      throw BadRequestException(Message.INVITATION_ORGANIZATION_MISMATCH)
    }

    if (!organizationRoleService.isUserMemberOrOwner(user.id, org.id)) {
      organizationRoleService.grantMemberRoleToUser(user, org)
    }
    organizationRoleService.setManaged(user, org, true)
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

  private fun createUserEntity(
    userResponse: OAuthUserDetails,
    thirdPartyAuthType: ThirdPartyAuthType,
    accountType: UserAccount.AccountType,
  ): UserAccount {
    val user = UserAccount()
    user.username = userResponse.email

    val name =
      userResponse.name ?: run {
        if (userResponse.givenName != null && userResponse.familyName != null) {
          "${userResponse.givenName} ${userResponse.familyName}"
        } else {
          userResponse.email.split("@")[0]
        }
      }
    user.name = name
    user.thirdPartyAuthId = userResponse.sub
    user.thirdPartyAuthType = thirdPartyAuthType
    user.ssoRefreshToken = userResponse.refreshToken
    user.accountType = accountType
    user.ssoSessionExpiry = userAccountService.getCurrentSsoExpiration(thirdPartyAuthType)
    return user
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

    val user =
      createUserEntity(
        userResponse,
        thirdPartyAuthType,
        accountType,
      )

    signUpService.signUp(user, invitationCode, null)
    checkUserManagingOrganization(userResponse, user)
    return user
  }
}
