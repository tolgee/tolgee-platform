package io.tolgee.service.security

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
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
import java.util.UUID

@Service
class AuthProviderChangeService(
  private val tolgeeProperties: TolgeeProperties,
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
    return user.asAuthProviderDto(tolgeeProperties)
  }

  fun getRequestedChange(userAccount: UserAccount): AuthProviderDto? {
    return getActiveAuthProviderChangeRequest(userAccount)?.asAuthProviderDto()
  }

  /**
   * Alters authentication flow of third party authentication to allow initiating third party provider change
   * for already authenticated user
   */
  fun tryInitiate(
    authenticatingUser: UserAccount?,
    userEmail: String,
    data: AuthProviderChangeData,
  ) {
    val currentUser = authenticationFacade.authenticatedUserOrNull
    val matchingUser = userAccountService.findActive(userEmail)

    when {
      currentUser == null -> {
        // Nobody to initiate the change for
      }

      authenticatingUser?.id == currentUser.id -> {
        // Nothing to change
        // User used the same method for authentication as they already use - we can initiate
        // provider change but there is nothing to change
      }

      authenticatingUser != null -> {
        // Non-matching e-mail
        // Account with this e-mail already exists and would be authenticated otherwise
        throw AuthenticationException(Message.THIRD_PARTY_SWITCH_CONFLICT)
      }

      matchingUser == null -> {
        // Non-matching e-mail and no account with this email exists
        throw AuthenticationException(Message.THIRD_PARTY_SWITCH_CONFLICT)
      }

      matchingUser.id != currentUser.id -> {
        // Non-matching e-mail and account with this e-mail already exists
        throw AuthenticationException(Message.THIRD_PARTY_SWITCH_CONFLICT)
      }

      else -> {
        self.saveInNewTransaction(matchingUser, data)
        throw AuthenticationException(Message.THIRD_PARTY_SWITCH_INITIATED)
      }
    }
  }

  /**
   * Force save the change request - will not roll back with parent transaction
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected fun saveInNewTransaction(
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
    self.save(request.from(user, data))
  }

  fun save(request: AuthProviderChangeRequest) {
    authProviderChangeRequestRepository.save(request)
  }

  fun apply(
    user: UserAccount,
    data: AuthProviderChangeData,
  ) {
    self.apply(AuthProviderChangeRequest().from(user, data))
  }

  fun initiateRemove(user: UserAccount) {
    val data = AuthProviderChangeData(null)
    self.save(user, data)
  }

  @Transactional
  fun accept(
    userAccount: UserAccount,
    id: String?,
  ) {
    val req = getActiveAuthProviderChangeRequest(userAccount) ?: throw NotFoundException()
    if (id != null && req.identifier != id) {
      throw NotFoundException()
    }

    self.apply(req)
    authProviderChangeRequestRepository.delete(req)
  }

  @Transactional
  fun apply(req: AuthProviderChangeRequest) {
    val userAccount = req.userAccount ?: return throw NotFoundException()
    if (userAccount.accountType === UserAccount.AccountType.MANAGED) {
      throw BadRequestException(Message.OPERATION_UNAVAILABLE_FOR_ACCOUNT_TYPE)
    }

    if (req.authType == null && userAccount.password == null) {
      throw BadRequestException(Message.USER_MISSING_PASSWORD)
    }

    checkEnabled(req.authType)

    userAccount.applyRequest(req)
  }

  private fun checkEnabled(provider: ThirdPartyAuthType?) {
    val providerEnabled =
      when (provider) {
        ThirdPartyAuthType.SSO_GLOBAL -> tolgeeProperties.authentication.ssoGlobal.enabled
        ThirdPartyAuthType.SSO -> tolgeeProperties.authentication.ssoOrganizations.enabled
        ThirdPartyAuthType.GITHUB -> tolgeeProperties.authentication.github.clientId != null
        ThirdPartyAuthType.GOOGLE -> tolgeeProperties.authentication.google.clientId != null
        ThirdPartyAuthType.OAUTH2 -> tolgeeProperties.authentication.oauth2.clientId != null
        null -> true // native login
      }

    if (!providerEnabled) {
      // Don't allow user to switch to disabled authentication provider
      throw BadRequestException(Message.AUTHENTICATION_METHOD_DISABLED)
    }
  }

  private fun UserAccount.applyRequest(req: AuthProviderChangeRequest) {
    accountType = accountType.transformWith(req)
    thirdPartyAuthType = req.authType
    thirdPartyAuthId = req.authId
    ssoRefreshToken = req.ssoRefreshToken
    ssoSessionExpiry = req.ssoExpiration
    userAccountService.save(this)

    resetEmailVerification()
    resetNativeAuth()
    applyDomain(req)
  }

  private fun UserAccount.AccountType?.transformWith(req: AuthProviderChangeRequest): UserAccount.AccountType? {
    return when (req.authType) {
      ThirdPartyAuthType.SSO, ThirdPartyAuthType.SSO_GLOBAL -> UserAccount.AccountType.MANAGED
      ThirdPartyAuthType.GOOGLE, ThirdPartyAuthType.GITHUB, ThirdPartyAuthType.OAUTH2 -> this
      null -> UserAccount.AccountType.LOCAL
    }
  }

  private fun UserAccount.resetEmailVerification() {
    if (thirdPartyAuthType == null) {
      return
    }

    // User authenticated using current email - it is verified
    emailVerification = null
  }

  private fun UserAccount.resetNativeAuth() {
    if (accountType != UserAccount.AccountType.MANAGED) {
      return
    }

    // When switching user to MANAGED mode
    // we clear all the native authentication details
    // this way user can no longer use them with API endpoints
    // that doesn't check if user is MANAGED
    password = null
    totpKey = null
    mfaRecoveryCodes = emptyList()
    userAccountService.save(this)
  }

  private fun UserAccount.applyDomain(req: AuthProviderChangeRequest) {
    val userAccount = this
    val domain = req.ssoDomain
    if (domain == null) {
      return
    }

    if (userAccount.domain != domain) {
      // User probably changed his email between change request creation and the apply call
      throw BadRequestException(Message.THIRD_PARTY_SWITCH_CONFLICT)
    }

    if (req.authType != ThirdPartyAuthType.SSO) {
      if (req.authType != ThirdPartyAuthType.SSO_GLOBAL) {
        // Shouldn't happen
        throw IllegalStateException(
          "AuthProviderChangeRequest with ssoDomain set (${req.ssoDomain} but non-SSO authType: ${req.authType}",
        )
      }
      return
    }

    val tenant = tenantService.getEnabledConfigByDomain(domain)

    val organizationId = tenant.organizationId
    if (organizationId == null) {
      throw NotFoundException(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED)
    }

    val organization = organizationService.find(organizationId)
    if (organization == null) {
      throw NotFoundException(Message.ORGANIZATION_NOT_FOUND)
    }

    if (!organizationRoleService.hasAnyOrganizationRole(userAccount.id, organization.id)) {
      organizationRoleService.grantMemberRoleToUser(userAccount, organization)
    }
    organizationRoleService.setManaged(userAccount, organization, true)
  }

  @Transactional
  fun reject(userAccount: UserAccount) {
    val req = getActiveAuthProviderChangeRequest(userAccount) ?: throw NotFoundException()
    authProviderChangeRequestRepository.delete(req)
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
    identifier = UUID.randomUUID().toString()
    authType = data.authType
    authId = data.authId
    ssoDomain = data.ssoDomain
    ssoRefreshToken = data.ssoRefreshToken
    ssoExpiration = data.ssoExpiration
    return this
  }
}
