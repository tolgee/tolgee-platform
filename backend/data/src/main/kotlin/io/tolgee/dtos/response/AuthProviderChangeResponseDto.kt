package io.tolgee.dtos.response

import io.tolgee.model.AuthProviderChangeRequest

data class AuthProviderChangeResponseDto(
  var newAuthType: String?,
  var oldAuthType: String?,
  var newAccountType: String?,
  var userId: Long?,
  var oldAccountType: String?,
) {
  companion object {
    fun fromEntity(entity: AuthProviderChangeRequest): AuthProviderChangeResponseDto =
      AuthProviderChangeResponseDto(
        newAuthType = entity.newAuthProvider?.code(),
        oldAuthType = entity.oldAuthProvider?.code(),
        newAccountType = entity.newAccountType?.name,
        userId = entity.userAccount?.id,
        oldAccountType = entity.userAccount?.accountType?.name,
      )
  }
}
