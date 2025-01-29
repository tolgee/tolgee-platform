package io.tolgee.service.security

import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.AuthProviderChangeData
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.repository.AuthProviderChangeRequestRepository
import org.springframework.stereotype.Service

@Service
class AuthProviderChangeService(
  private val authProviderChangeRequestRepository: AuthProviderChangeRequestRepository,
) {
  fun initiateProviderChange(data: AuthProviderChangeData): Nothing {
    authProviderChangeRequestRepository.deleteByUserAccountId(data.userAccount.id)
    authProviderChangeRequestRepository.save(data.asAuthProviderChangeRequest())
    throw AuthenticationException(Message.USERNAME_ALREADY_EXISTS) // TODO: separate exception
  }
}
