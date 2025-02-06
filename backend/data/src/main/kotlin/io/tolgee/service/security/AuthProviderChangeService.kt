package io.tolgee.service.security

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.AuthProviderChangeData
import io.tolgee.dtos.response.AuthProviderDto
import io.tolgee.dtos.response.AuthProviderDto.Companion.asAuthProviderDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.repository.AuthProviderChangeRequestRepository
import io.tolgee.service.TenantService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.util.addMinutes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AuthProviderChangeService(
  private val authProviderChangeRequestRepository: AuthProviderChangeRequestRepository,
  private val tenantService: TenantService,
  private val organizationRoleService: OrganizationRoleService,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun getCurrent(user: UserAccount): AuthProviderDto? {
    return user.asAuthProviderDto()
  }

  fun getRequestedChange(userAccount: UserAccount): AuthProviderDto? {
    return getActiveAuthProviderChangeRequest(userAccount)?.asAuthProviderDto()
  }

  fun initiateProviderChange(data: AuthProviderChangeData) {
    if (data.accountType == UserAccount.AccountType.MANAGED) {
      // Immediately accept auth provider change requests for SSO accounts
      val expirationDate = currentDateProvider.date.addMinutes(30)
      val change = data.asAuthProviderChangeRequest(expirationDate)
      acceptProviderChange(change)
      return
    }

    // Changing authentication provider to non-SSO providers is disabled for now
    // selfReference.saveProviderChange(data)
    // throw AuthenticationException(Message.THIRD_PARTY_SWITCH_INITIATED)

    throw AuthenticationException(Message.USERNAME_ALREADY_EXISTS)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun saveProviderChange(data: AuthProviderChangeData) {
    authProviderChangeRequestRepository.deleteByUserAccountId(data.userAccount.id)
    val expirationDate = currentDateProvider.date.addMinutes(30)
    authProviderChangeRequestRepository.save(data.asAuthProviderChangeRequest(expirationDate))
  }

  fun acceptProviderChange(userAccount: UserAccount) {
    val req = getActiveAuthProviderChangeRequest(userAccount) ?: return
    acceptProviderChange(req)
    authProviderChangeRequestRepository.delete(req)
  }

  fun acceptProviderChange(req: AuthProviderChangeRequest) {
    val userAccount = req.userAccount ?: return throw NotFoundException()
    if (userAccount.accountType === UserAccount.AccountType.MANAGED) {
      throw AuthenticationException(Message.OPERATION_UNAVAILABLE_FOR_ACCOUNT_TYPE)
    }

    userAccount.apply {
      accountType = req.accountType
      thirdPartyAuthType = req.authType
      thirdPartyAuthId = req.authId
      ssoRefreshToken = req.ssoRefreshToken
      ssoSessionExpiry = req.ssoExpiration
    }

    val domain = req.ssoDomain
    if (domain != null && req.authType == ThirdPartyAuthType.SSO) {
      val tenant = tenantService.getEnabledConfigByDomain(domain)
      val organization = tenant.organization
      if (organization == null) {
        throw NotFoundException(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED)
      }
      if (!organizationRoleService.isUserMemberOrOwner(userAccount.id, organization.id)) {
        organizationRoleService.grantMemberRoleToUser(userAccount, organization)
      }
      organizationRoleService.setManaged(userAccount, organization, true)
    }
  }

  fun rejectProviderChange(userAccount: UserAccount) {
    authProviderChangeRequestRepository.deleteByUserAccountId(userAccount.id)
  }

  private fun getActiveAuthProviderChangeRequest(userAccount: UserAccount): AuthProviderChangeRequest? {
    val request = userAccount.authProviderChangeRequest ?: return null
    val expiry = request.expirationDate ?: return null
    if (expiry < currentDateProvider.date) {
      return null
    }
    return request
  }
}
