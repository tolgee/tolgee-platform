package io.tolgee.dtos.cacheable

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import java.io.Serializable
import java.util.Date

data class UserAccountDto(
  val name: String,
  val username: String,
  val domain: String?,
  val role: UserAccount.Role?,
  val id: Long,
  val needsSuperJwt: Boolean,
  val avatarHash: String?,
  val deleted: Boolean,
  val tokensValidNotBefore: Date?,
  val emailVerified: Boolean,
  val thirdPartyAuth: ThirdPartyAuthType?,
  val ssoRefreshToken: String?,
  val ssoSessionExpiry: Date?,
) : Serializable {
  companion object {
    fun fromEntity(entity: UserAccount) =
      UserAccountDto(
        name = entity.name,
        username = entity.username,
        domain = entity.domain,
        role = entity.role,
        id = entity.id,
        needsSuperJwt = entity.needsSuperJwt,
        avatarHash = entity.avatarHash,
        deleted = entity.deletedAt != null,
        tokensValidNotBefore = entity.tokensValidNotBefore,
        emailVerified = entity.emailVerification == null,
        thirdPartyAuth = entity.thirdPartyAuthType,
        ssoRefreshToken = entity.ssoRefreshToken,
        ssoSessionExpiry = entity.ssoSessionExpiry,
      )
  }

  override fun toString(): String {
    return username
  }
}

fun UserAccountDto.isAdmin(): Boolean {
  return role == UserAccount.Role.ADMIN
}

fun UserAccountDto.isSupporter(): Boolean {
  return role == UserAccount.Role.SUPPORTER
}

fun UserAccountDto.isSupporterOrAdmin(): Boolean {
  return role == UserAccount.Role.SUPPORTER || role == UserAccount.Role.ADMIN
}
