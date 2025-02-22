package io.tolgee.service.security

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.AuthProviderChangeData
import io.tolgee.dtos.response.AuthProviderDto
import io.tolgee.dtos.response.AuthProviderDto.Companion.asAuthProviderDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.repository.AuthProviderChangeRequestRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.TenantService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.util.addMinutes
import org.apache.commons.lang3.time.DateUtils
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.Calendar

@Service
class AuthProviderChangeService(
  private val authProviderChangeRequestRepository: AuthProviderChangeRequestRepository,
  private val tenantService: TenantService,
  private val userAccountService: UserAccountService,
  private val organizationRoleService: OrganizationRoleService,
  private val currentDateProvider: CurrentDateProvider,
  private val authenticationFacade: AuthenticationFacade,
  @Suppress("SelfReferenceConstructorParameter") @Lazy
  private val self: AuthProviderChangeService,
) {
  fun getCurrent(user: UserAccount): AuthProviderDto? {
    return user.asAuthProviderDto()
  }

  fun getRequestedChange(userAccount: UserAccount): AuthProviderDto? {
    return getActiveAuthProviderChangeRequest(userAccount)?.asAuthProviderDto()
  }

  fun initiate(
    resolvedUser: UserAccount?,
    userEmail: String,
    data: AuthProviderChangeData,
  ) {
    val currentUser = authenticationFacade.authenticatedUserOrNull

    if (currentUser == null) {
      // Provider change can be only initiated for already signed-in user
      return
    }

    if (resolvedUser?.id == currentUser.id) {
      // Trying to log-in as already logged-in user??
      return
    }

    val targetUser = resolvedUser ?: userAccountService.findActive(userEmail)
    if (targetUser?.id != currentUser.id) {
      // User is trying to switch third party authentication provider, but
      // the account e-mail does not match the email of the third party account
      throw AuthenticationException(Message.THIRD_PARTY_SWITCH_CONFLICT)
    }

    self.save(targetUser, data)
    throw AuthenticationException(Message.THIRD_PARTY_SWITCH_INITIATED)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun save(
    user: UserAccount,
    data: AuthProviderChangeData,
  ) {
    authProviderChangeRequestRepository.deleteByUserAccountId(user.id)
    authProviderChangeRequestRepository.save(data.asAuthProviderChangeRequest(user))
  }

  fun apply(
    user: UserAccount,
    data: AuthProviderChangeData,
  ) {
    val change = data.asAuthProviderChangeRequest(user)
    self.apply(change)
  }

  fun removeCurrent(user: UserAccount) {
    if (user.password == null) {
      throw BadRequestException(Message.USER_MISSING_PASSWORD)
    }
    self.apply(
      AuthProviderChangeData(
        UserAccount.AccountType.LOCAL,
        null,
      ).asAuthProviderChangeRequest(user),
    )
  }

  @Transactional
  fun accept(userAccount: UserAccount) {
    val req = getActiveAuthProviderChangeRequest(userAccount) ?: return
    self.apply(req)
    authProviderChangeRequestRepository.delete(req)
  }

  @Transactional
  fun apply(req: AuthProviderChangeRequest) {
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
    userAccountService.save(userAccount)

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

  @Transactional
  fun reject(userAccount: UserAccount) {
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

  fun AuthProviderChangeData.asAuthProviderChangeRequest(user: UserAccount): AuthProviderChangeRequest {
    val expirationDate = currentDateProvider.date.addMinutes(30)
    return AuthProviderChangeRequest().also {
      it.userAccount = user
      it.expirationDate = DateUtils.truncate(expirationDate, Calendar.SECOND)
      it.accountType = this.accountType
      it.authType = this.authType
      it.authId = this.authId
      it.ssoDomain = this.ssoDomain
      it.ssoRefreshToken = this.ssoRefreshToken
      it.ssoExpiration = this.ssoExpiration
    }
  }
}
