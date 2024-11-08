package io.tolgee.dtos.request

data class AuthProviderChangeRequestDto(
  val isConfirmed: Boolean,
  val changeRequestId: Long
)
