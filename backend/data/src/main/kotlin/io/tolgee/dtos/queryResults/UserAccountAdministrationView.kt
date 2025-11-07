package io.tolgee.dtos.queryResults

import io.tolgee.api.IUserAccount
import io.tolgee.model.UserAccount
import java.util.Date

data class UserAccountAdministrationView(
  val id: Long,
  override val username: String,
  val name: String,
  val emailAwaitingVerification: String?,
  val avatarHash: String?,
  @Deprecated("See `UserAccount` for details.")
  /** @see [io.tolgee.model.UserAccount.thirdPartyAuthType] */
  override val accountType: UserAccount.AccountType?,
  val role: UserAccount.Role?,
  override var isInitialUser: Boolean,
  override val totpKey: ByteArray?,
  val deletedAt: Date?,
  val disabledAt: Date?,
  val lastActivity: Date?,
) : IUserAccount
