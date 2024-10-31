package io.tolgee.service.security

import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserCredentialsService(
  private val passwordEncoder: PasswordEncoder,
) {
  @set:Autowired
  lateinit var userAccountService: UserAccountService

  fun checkUserCredentials(
    username: String,
    password: String,
  ): UserAccount {
    val userAccount =
      userAccountService.findActive(username)
        ?: throw AuthenticationException(Message.BAD_CREDENTIALS)

    if (userAccount.accountType == UserAccount.AccountType.MANAGED) {
      throw AuthenticationException(Message.OPERATION_UNAVAILABLE_FOR_ACCOUNT_TYPE)
    }

    if (userAccount.thirdPartyAuthType == ThirdPartyAuthType.SSO) {
      throw AuthenticationException(Message.SSO_USER_OPERATION_UNAVAILABLE)
    }

    checkNativeUserCredentials(userAccount, password)
    return userAccount
  }

  fun checkUserCredentials(
    user: UserAccount,
    password: String,
  ) {
    checkNativeUserCredentials(user, password)
  }

  private fun checkNativeUserCredentials(
    user: UserAccount,
    password: String,
  ) {
    if (!passwordEncoder.matches(password, user.password)) {
      throw AuthenticationException(Message.BAD_CREDENTIALS)
    }
  }
}
