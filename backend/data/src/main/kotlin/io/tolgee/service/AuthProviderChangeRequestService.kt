package io.tolgee.service

import io.tolgee.dtos.request.AuthProviderChangeRequestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.repository.AuthProviderChangeRequestRepository
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthProviderChangeRequestService(
  private val authProviderChangeRequestRepository: AuthProviderChangeRequestRepository,
  private val userAccountService: UserAccountService,
  private val tenantService: EeSsoTenantService
) {
  fun getById(id: Long): AuthProviderChangeRequest = findById(id).orElseGet { throw NotFoundException() }

  fun findById(id: Long): Optional<AuthProviderChangeRequest> = authProviderChangeRequestRepository.findById(id)

  fun findByUserAccount(userAccountId: Long): Optional<AuthProviderChangeRequest> =
    authProviderChangeRequestRepository.findByUserAccountId(userAccountId)

  fun getByUserAccountId(userAccountId: Long): AuthProviderChangeRequest =
    findByUserAccount(userAccountId).orElseGet { throw NotFoundException() }

  fun create(
    userAccount: UserAccount,
    newAuthProvider: ThirdPartyAuthType?,
    oldAuthProvider: ThirdPartyAuthType?,
    newAccountType: UserAccount.AccountType,
    oldAccountType: UserAccount.AccountType?,
    ssoDomain: String?,
    sub: String?,
    refreshToken: String?,
    calculateExpirationDate: Date
  ): AuthProviderChangeRequest {
    findByUserAccount(userAccount.id).ifPresent {
      throw BadRequestException("User already has a pending auth provider change request")
    }

    val authProviderChangeRequest =
      AuthProviderChangeRequest().apply {
        this.userAccount = userAccount
        this.newAuthProvider = newAuthProvider
        this.oldAuthProvider = oldAuthProvider
        this.newAccountType = newAccountType
        this.oldAccountType = oldAccountType
        this.newSsoDomain = ssoDomain
        this.newSub = sub
        this.ssoRefreshToken = refreshToken
        this.ssoExpiration = calculateExpirationDate
      }
    val saved = authProviderChangeRequestRepository.save(authProviderChangeRequest)
    userAccount.authProviderChangeRequest = saved
    userAccountService.save(userAccount)
    return saved
  }

  fun submitOrCancel(authProviderChangeRequestDto: AuthProviderChangeRequestDto) {
    val entity = getById(authProviderChangeRequestDto.changeRequestId)
    if(!authProviderChangeRequestDto.isConfirmed) {
      authProviderChangeRequestRepository.delete(entity)
      return
    }
    entity.isConfirmed = true
    authProviderChangeRequestRepository.save(entity)
  }

  fun resolveChangeRequestIfExist(userAccount: UserAccount) {
    findByUserAccount(userAccount.id).ifPresent {
      if (it.isConfirmed) {
        userAccount.accountType = it.newAccountType
        userAccount.thirdPartyAuthType = it.newAuthProvider
        userAccount.ssoTenant = if (it.newSsoDomain != null) {
          tenantService.getByDomain(it.newSsoDomain!!)
        } else {
          null
        }
        userAccount.thirdPartyAuthId = it.newSub
        userAccount.ssoRefreshToken = it.ssoRefreshToken
        userAccount.ssoSessionExpiry = it.ssoExpiration
        userAccountService.save(userAccount)
        authProviderChangeRequestRepository.delete(it)
      }
    }
  }
}
