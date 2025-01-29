package io.tolgee.service.security

import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.AuthProviderChangeData
import io.tolgee.dtos.response.AuthProviderDto
import io.tolgee.dtos.response.AuthProviderDto.Companion.asAuthProviderDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.repository.AuthProviderChangeRequestRepository
import io.tolgee.service.organization.OrganizationService
import org.springframework.stereotype.Service

@Service
class AuthProviderChangeService(
  private val authProviderChangeRequestRepository: AuthProviderChangeRequestRepository,
  private val organizationService: OrganizationService,
) {
  fun getCurrent(user: UserAccount): AuthProviderDto? {
    return user.asAuthProviderDto()
  }

  fun getRequestedChange(user: UserAccount): AuthProviderDto? {
    return user.authProviderChangeRequest?.asAuthProviderDto()
  }

  fun initiateProviderChange(data: AuthProviderChangeData): Nothing {
    // Note: Is it ok to use Nothing method in this case? Feels like readability might suffer...
    // Maybe I can return the exception and throw it from caller or something like that.
    authProviderChangeRequestRepository.deleteByUserAccountId(data.userAccount.id)
    authProviderChangeRequestRepository.save(data.asAuthProviderChangeRequest())
    throw AuthenticationException(Message.THIRD_PARTY_SWITCH_INITIATED)
  }

  fun acceptProviderChange(userAccount: UserAccount) {
    if (userAccount.accountType === UserAccount.AccountType.MANAGED) {
      throw AuthenticationException(Message.OPERATION_UNAVAILABLE_FOR_ACCOUNT_TYPE)
    }

    // TODO: provider change request expiration

    val req = userAccount.authProviderChangeRequest ?: return
    userAccount.apply {
      thirdPartyAuthType = req.authType
      thirdPartyAuthId = req.authId
      ssoRefreshToken = req.ssoRefreshToken
      ssoSessionExpiry = req.ssoExpiration
    }
    req.ssoDomain // TODO: apply tenant changes
    authProviderChangeRequestRepository.delete(req)
  }

  fun rejectProviderChange(userAccount: UserAccount) {
    authProviderChangeRequestRepository.deleteByUserAccountId(userAccount.id)
  }
}
