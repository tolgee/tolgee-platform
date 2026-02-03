package io.tolgee.security.thirdParty

import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.AuthProviderChangeData
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.UserAccount.AccountType
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.thirdParty.data.ThirdPartyUserDetails
import io.tolgee.service.TenantService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.AuthProviderChangeService
import io.tolgee.service.security.SignUpService
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Component

@Component
class ThirdPartyUserHandler(
  private val signUpService: SignUpService,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
  private val authProviderChangeService: AuthProviderChangeService,
  private val tenantService: TenantService,
) {
  fun findOrCreateUser(data: ThirdPartyUserDetails): UserAccount {
    val userAccount = getUserAccount(data)

    authProviderChangeService.tryInitiate(
      userAccount,
      data.username,
      AuthProviderChangeData(
        data.thirdPartyAuthType,
        data.authId,
        ssoDomain = data.tenant?.domain,
        ssoRefreshToken = data.refreshToken,
        ssoExpiration = userAccountService.getCurrentSsoExpiration(data.thirdPartyAuthType),
      ),
    )

    if (userAccount != null) {
      if (data.thirdPartyAuthType in arrayOf(ThirdPartyAuthType.SSO, ThirdPartyAuthType.SSO_GLOBAL)) {
        userAccountService.updateSsoSession(userAccount, data.refreshToken)
      }
      return userAccount
    }

    return createUser(data)
  }

  private fun getManagingOrganization(data: ThirdPartyUserDetails): Organization? {
    return data.tenant?.organizationId?.let {
      organizationService.find(it) ?: throw AuthenticationException(Message.ORGANIZATION_NOT_FOUND)
    }
  }

  private fun checkNotManagedByOrganization(domain: String?) {
    if (tenantService.getEnabledConfigByDomainOrNull(domain)?.organizationId != null) {
      // There is sso configured for the domain - don't allow sign up without sso
      throw AuthenticationException(Message.USE_SSO_FOR_AUTHENTICATION_INSTEAD, listOf(domain))
    }
  }

  private fun checkUserManagingOrganization(
    data: ThirdPartyUserDetails,
    user: UserAccount,
  ) {
    val org = getManagingOrganization(data)
    if (org == null) {
      checkNotManagedByOrganization(user.domain)
      return
    }

    if (user.organizationRoles.any { it.organization?.id != org.id }) {
      // User accepted invitation for different organization
      throw BadRequestException(Message.INVITATION_ORGANIZATION_MISMATCH)
    }

    if (!organizationRoleService.hasAnyOrganizationRole(user.id, org.id)) {
      organizationRoleService.grantMemberRoleToUser(user, org)
    }
    organizationRoleService.setManaged(user, org, true)
  }

  private fun getUserAccount(data: ThirdPartyUserDetails): UserAccount? {
    if (data.thirdPartyAuthType == ThirdPartyAuthType.SSO) {
      if (data.tenant == null) {
        // This should never happen
        throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)
      }
      return userAccountService.findEnabledBySsoDomain(data.tenant.domain, data.authId)
    }
    return userAccountService.findByThirdParty(data.thirdPartyAuthType, data.authId)
  }

  private fun createUserEntity(data: ThirdPartyUserDetails): UserAccount {
    val user = UserAccount()
    user.username = data.username
    user.name = data.name
    user.thirdPartyAuthId = data.authId
    user.thirdPartyAuthType = data.thirdPartyAuthType
    user.ssoRefreshToken = data.refreshToken
    user.accountType = guessAccountType(data)
    user.ssoSessionExpiry = userAccountService.getCurrentSsoExpiration(data.thirdPartyAuthType)
    return user
  }

  private fun guessAccountType(data: ThirdPartyUserDetails): AccountType {
    return when (data.thirdPartyAuthType) {
      ThirdPartyAuthType.SSO, ThirdPartyAuthType.SSO_GLOBAL -> AccountType.MANAGED
      ThirdPartyAuthType.GITHUB, ThirdPartyAuthType.OAUTH2, ThirdPartyAuthType.GOOGLE -> AccountType.THIRD_PARTY
    }
  }

  private fun createUser(data: ThirdPartyUserDetails): UserAccount {
    userAccountService.findActive(data.username)?.let {
      throw AuthenticationException(Message.USERNAME_ALREADY_EXISTS)
    }

    val user = createUserEntity(data)

    signUpService.signUp(user, data.invitationCode, null)
    checkUserManagingOrganization(data, user)
    return user
  }
}
