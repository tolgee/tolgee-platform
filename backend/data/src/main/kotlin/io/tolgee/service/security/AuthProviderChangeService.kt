package io.tolgee.service.security

import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.AuthProviderChangeData
import io.tolgee.dtos.response.AuthProviderDto
import io.tolgee.dtos.response.AuthProviderDto.Companion.asAuthProviderDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.repository.AuthProviderChangeRequestRepository
import io.tolgee.service.TenantService
import io.tolgee.service.organization.OrganizationRoleService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AuthProviderChangeService(
  private val authProviderChangeRequestRepository: AuthProviderChangeRequestRepository,
  private val tenantService: TenantService,
  private val organizationRoleService: OrganizationRoleService,
) {
  fun getCurrent(user: UserAccount): AuthProviderDto? {
    return user.asAuthProviderDto()
  }

  fun getRequestedChange(user: UserAccount): AuthProviderDto? {
    return user.authProviderChangeRequest?.asAuthProviderDto()
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun initiateProviderChange(data: AuthProviderChangeData) {
    authProviderChangeRequestRepository.deleteByUserAccountId(data.userAccount.id)
    authProviderChangeRequestRepository.save(data.asAuthProviderChangeRequest())
  }

  fun acceptProviderChange(userAccount: UserAccount) {
    if (userAccount.accountType === UserAccount.AccountType.MANAGED) {
      throw AuthenticationException(Message.OPERATION_UNAVAILABLE_FOR_ACCOUNT_TYPE)
    }

    val req = userAccount.authProviderChangeRequest ?: return
    userAccount.apply {
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
    authProviderChangeRequestRepository.delete(req)
  }

  fun rejectProviderChange(userAccount: UserAccount) {
    authProviderChangeRequestRepository.deleteByUserAccountId(userAccount.id)
  }
}
