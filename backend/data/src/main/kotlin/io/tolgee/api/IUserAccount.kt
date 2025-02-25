package io.tolgee.api

import io.tolgee.model.UserAccount

interface IUserAccount {
  val username: String
  var isInitialUser: Boolean

  val isDeletable: Boolean
    get() = this.accountType != UserAccount.AccountType.MANAGED && !this.isInitialUser

  val isMfaEnabled: Boolean
    get() = this.totpKey?.isNotEmpty() ?: false

  val needsSuperJwt: Boolean
    get() = this.accountType == UserAccount.AccountType.LOCAL || isMfaEnabled

  val domain: String?
  get() {
    val username = this.username
    val valid = username.count { it == '@' } == 1
    if (!valid) {
      return null
    }
    val (_, domain) = username.split('@')
    return domain
  }

  val totpKey: ByteArray?

  val accountType: UserAccount.AccountType?
}
