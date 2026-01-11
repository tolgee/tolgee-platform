package io.tolgee.dtos.response

import io.tolgee.model.UserAccount

data class UserResponseDTO(
  var id: Long? = null,
  var name: String? = null,
  var username: String? = null,
  var emailAwaitingVerification: String? = null,
) {
  companion object {
    fun fromEntity(userAccount: UserAccount): UserResponseDTO {
      return UserResponseDTO(
        name = userAccount.name,
        username = userAccount.username,
        id = userAccount.id,
        emailAwaitingVerification = userAccount.emailVerification?.newEmail,
      )
    }
  }
}
