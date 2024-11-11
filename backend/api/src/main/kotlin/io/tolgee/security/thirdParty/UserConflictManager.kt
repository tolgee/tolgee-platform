package io.tolgee.security.thirdParty

import io.tolgee.constants.Message
import io.tolgee.dtos.AuthProviderChangeRequestData
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.service.AuthProviderChangeRequestService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserConflictManager(
  @Lazy
  private val authProviderChangeRequestService: AuthProviderChangeRequestService,
) {
  fun manageUserNameConflict(
    user: UserAccount,
    newAuthProvider: ThirdPartyAuthType,
    newAccountType: UserAccount.AccountType,
    ssoDomain: String? = null,
    sub: String? = null,
    refreshToken: String? = null,
    calculateExpirationDate: Date? = null,
  ) {
    val requestData =
      AuthProviderChangeRequestData(
        userAccount = user,
        newAuthProvider = newAuthProvider,
        oldAuthProvider = user.thirdPartyAuthType,
        newAccountType = newAccountType,
        oldAccountType = user.accountType,
        ssoDomain = ssoDomain,
        sub = sub,
        refreshToken = refreshToken,
        calculateExpirationDate = calculateExpirationDate,
      )
    val request = authProviderChangeRequestService.create(requestData)
    throw AuthenticationException(Message.USERNAME_ALREADY_EXISTS, listOf(request?.id))
  }

  fun resolveRequestIfExist(user: Optional<UserAccount>) {
    if (user.isPresent) {
      authProviderChangeRequestService.resolveChangeRequestIfExist(user.get())
    }
  }
}
