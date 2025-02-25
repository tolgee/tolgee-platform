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
import io.tolgee.service.organization.OrganizationService
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
  private val organizationService: OrganizationService,
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

    self.forceSave(targetUser, data)
    throw AuthenticationException(Message.THIRD_PARTY_SWITCH_INITIATED)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected fun forceSave(
    user: UserAccount,
    data: AuthProviderChangeData,
  ) {
    self.save(user, data)
  }

  @Transactional
  fun save(
    user: UserAccount,
    data: AuthProviderChangeData,
  ) {
    val existing = authProviderChangeRequestRepository.findByUserAccountId(user.id)
    val request = existing ?: AuthProviderChangeRequest()
    authProviderChangeRequestRepository.save(request.from(user, data))
  }

  fun apply(
    user: UserAccount,
    data: AuthProviderChangeData,
  ) {
    self.apply(AuthProviderChangeRequest().from(user, data))
  }

  fun removeCurrent(user: UserAccount) {
    if (user.password == null) {
      throw BadRequestException(Message.USER_MISSING_PASSWORD)
    }
    val data =
      AuthProviderChangeData(
        UserAccount.AccountType.LOCAL,
        null,
      )
    self.apply(AuthProviderChangeRequest().from(user, data))
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
      val organizationId = tenant.organizationId
      if (organizationId == null) {
        throw NotFoundException(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED)
      }
      val organization =
        organizationService.find(organizationId)
          ?: throw NotFoundException(Message.ORGANIZATION_NOT_FOUND)
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

  private fun getActiveAuthProviderChangeRequest(user: UserAccount): AuthProviderChangeRequest? {
    val request = authProviderChangeRequestRepository.findByUserAccountId(user.id)
    val expiry = request?.expirationDate ?: return null
    if (expiry < currentDateProvider.date) {
      return null
    }
    return request
  }

  fun AuthProviderChangeRequest.from(
    user: UserAccount,
    data: AuthProviderChangeData,
  ): AuthProviderChangeRequest {
    val expiration = currentDateProvider.date.addMinutes(30)
    userAccount = user
    expirationDate = DateUtils.truncate(expiration, Calendar.SECOND)
    accountType = data.accountType
    authType = data.authType
    authId = data.authId
    ssoDomain = data.ssoDomain
    ssoRefreshToken = data.ssoRefreshToken
    ssoExpiration = data.ssoExpiration
    return this
  }
}
