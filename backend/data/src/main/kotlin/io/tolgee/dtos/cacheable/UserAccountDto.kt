package io.tolgee.dtos.cacheable

import io.tolgee.model.UserAccount
import java.io.Serializable

data class UserAccountDto(
  val name: String,
  val username: String,
  val role: UserAccount.Role?,
  val id: Long,
  val needsSuperJwt: Boolean
) : Serializable {
  companion object {
    fun fromEntity(entity: UserAccount) = UserAccountDto(
      name = entity.name,
      username = entity.username,
      role = entity.role,
      id = entity.id,
      needsSuperJwt = entity.needsSuperJwt
    )
  }

  override fun toString(): String {
    return username
  }
}
