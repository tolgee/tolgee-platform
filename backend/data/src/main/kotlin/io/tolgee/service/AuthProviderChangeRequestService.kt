package io.tolgee.service

import io.tolgee.dtos.AuthProviderChangeRequestData
import io.tolgee.dtos.request.AuthProviderChangeRequestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.UserAccount
import io.tolgee.repository.AuthProviderChangeRequestRepository
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AuthProviderChangeRequestService(
  private val authProviderChangeRequestRepository: AuthProviderChangeRequestRepository,
  private val userAccountService: UserAccountService,
) {
  fun getById(id: Long): AuthProviderChangeRequest = findById(id).orElseGet { throw NotFoundException() }

  fun findById(id: Long): Optional<AuthProviderChangeRequest> = authProviderChangeRequestRepository.findById(id)

  fun findByUserAccount(userAccountId: Long): Optional<AuthProviderChangeRequest> =
    authProviderChangeRequestRepository.findByUserAccountId(userAccountId)

  fun getByUserAccountId(userAccountId: Long): AuthProviderChangeRequest =
    findByUserAccount(userAccountId).orElseGet { throw NotFoundException() }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun create(data: AuthProviderChangeRequestData): AuthProviderChangeRequest? {
    findByUserAccount(data.userAccount.id).ifPresent {
      throw BadRequestException("User already has a pending auth provider change request")
    }
    val authProviderChangeRequest =
      AuthProviderChangeRequest().apply {
        this.userAccount = data.userAccount
        this.newAuthProvider = data.newAuthProvider
        this.oldAuthProvider = data.oldAuthProvider
        this.newAccountType = data.newAccountType
        this.oldAccountType = data.oldAccountType
        this.newSsoDomain = data.ssoDomain
        this.newSub = data.sub
        this.ssoRefreshToken = data.refreshToken
        this.ssoExpiration = data.calculateExpirationDate
      }

    val saved = authProviderChangeRequestRepository.save(authProviderChangeRequest)
    data.userAccount.authProviderChangeRequest = saved
    userAccountService.save(data.userAccount)

    return saved
  }

  fun confirmOrCancel(authProviderChangeRequestDto: AuthProviderChangeRequestDto) {
    val entity = getById(authProviderChangeRequestDto.changeRequestId)
    if (!authProviderChangeRequestDto.isConfirmed) {
      authProviderChangeRequestRepository.delete(entity)
      return
    }
    entity.isConfirmed = true
    authProviderChangeRequestRepository.save(entity)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun resolveChangeRequestIfExist(userAccount: UserAccount): Boolean {
    var wasResolved = false

    authProviderChangeRequestRepository.findByUserAccountAndIsConfirmed(userAccount, true).ifPresent {
      userAccount.accountType = it.newAccountType
      userAccount.thirdPartyAuthType = it.newAuthProvider
      userAccount.thirdPartyAuthId = it.newSub
      userAccount.ssoRefreshToken = it.ssoRefreshToken
      userAccount.ssoSessionExpiry = it.ssoExpiration
      userAccount.authProviderChangeRequest = null
      userAccountService.save(userAccount)
      authProviderChangeRequestRepository.delete(it)
      wasResolved = true
    }

    return wasResolved
  }
}
