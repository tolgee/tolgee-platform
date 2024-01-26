package io.tolgee.api

import io.tolgee.model.UserAccount

interface IUserAccount {
  var isInitialUser: Boolean
  val isDeletable: Boolean
    get() = this.accountType != UserAccount.AccountType.MANAGED && !this.isInitialUser

  val isMfaEnabled: Boolean
    get() = this.totpKey?.isNotEmpty() ?: false

  val needsSuperJwt: Boolean
    get() = this.accountType != UserAccount.AccountType.THIRD_PARTY || isMfaEnabled

  val totpKey: ByteArray?

  val accountType: UserAccount.AccountType?
}
