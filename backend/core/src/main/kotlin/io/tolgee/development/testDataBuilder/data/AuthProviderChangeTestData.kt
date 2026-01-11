package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.UserAccount
import io.tolgee.model.UserAccount.AccountType
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.util.addMinutes
import java.util.Date
import java.util.UUID

open class AuthProviderChangeTestData(
  currentDate: Date,
) : BaseTestData() {
  val validExpirationDate = currentDate.addMinutes(30)
  val expiredExpirationDate = currentDate.addMinutes(-30)

  var userNoProvider: UserAccount
  var userGithub: UserAccount
  var userChangeNoneToGithub: UserAccount
  var userChangeGithubToNone: UserAccount
  var userChangeGithubToNoneNoPassword: UserAccount
  var userChangeExpired: UserAccount

  lateinit var changeNoneToGithub: AuthProviderChangeRequest
  lateinit var changeGithubToNone: AuthProviderChangeRequest
  lateinit var changeGithubToNoneNoPassword: AuthProviderChangeRequest
  lateinit var changeExpired: AuthProviderChangeRequest

  init {
    userNoProvider =
      root
        .addUserAccount {
          username = "userNoProvider@domain.com"
        }.self
    userGithub =
      root
        .addUserAccount {
          username = "userGithub@domain.com"
          thirdPartyAuthType = ThirdPartyAuthType.GITHUB
          thirdPartyAuthId = "aaa1"
        }.self
    userChangeNoneToGithub =
      root
        .addUserAccount {
          username = "userChangeNoneToGithub@domain.com"
        }.build {
          changeNoneToGithub =
            setAuthProviderChangeRequest {
              identifier = UUID.randomUUID().toString()
              authType = ThirdPartyAuthType.GITHUB
              authId = "aaa2"
              expirationDate = validExpirationDate
            }.self
        }.self
    userChangeGithubToNone =
      root
        .addUserAccount {
          username = "userChangeGithubToNone@domain.com"
          thirdPartyAuthType = ThirdPartyAuthType.GITHUB
          thirdPartyAuthId = "aaa3"
        }.build {
          changeGithubToNone =
            setAuthProviderChangeRequest {
              identifier = UUID.randomUUID().toString()
              authType = null
              expirationDate = validExpirationDate
            }.self
        }.self
    userChangeGithubToNoneNoPassword =
      root
        .addUserAccount {
          username = "userChangeGithubToNoneNoPassowrd@domain.com"
          accountType = AccountType.THIRD_PARTY
          thirdPartyAuthType = ThirdPartyAuthType.GITHUB
          thirdPartyAuthId = "aaa4"
        }.build {
          rawPassword = null
          changeGithubToNoneNoPassword =
            setAuthProviderChangeRequest {
              identifier = UUID.randomUUID().toString()
              authType = null
              expirationDate = validExpirationDate
            }.self
        }.self
    userChangeExpired =
      root
        .addUserAccount {
          username = "userChangeExpired@domain.com"
        }.build {
          changeExpired =
            setAuthProviderChangeRequest {
              identifier = UUID.randomUUID().toString()
              authType = ThirdPartyAuthType.GITHUB
              authId = "aaa5"
              expirationDate = expiredExpirationDate
            }.self
        }.self
  }
}
