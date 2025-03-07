package io.tolgee.dtos.queryResults

import io.tolgee.api.IUserAccount
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType

class UserAccountView(
  val id: Long,
  override val username: String,
  val name: String,
  val emailAwaitingVerification: String?,
  val avatarHash: String?,
  override val accountType: UserAccount.AccountType?,
  val thirdPartyAuthType: ThirdPartyAuthType?,
  val role: UserAccount.Role?,
  override var isInitialUser: Boolean,
  override val totpKey: ByteArray?,
) : IUserAccount {
  companion object {
    fun fromEntity(entity: UserAccount): UserAccountView {
      return UserAccountView(
        id = entity.id,
        username = entity.username,
        name = entity.name,
        emailAwaitingVerification = entity.emailVerification?.newEmail,
        avatarHash = entity.avatarHash,
        accountType = entity.accountType ?: UserAccount.AccountType.LOCAL,
        thirdPartyAuthType = entity.thirdPartyAuthType,
        role = entity.role ?: UserAccount.Role.USER,
        isInitialUser = entity.isInitialUser,
        totpKey = entity.totpKey,
      )
    }
  }
}
