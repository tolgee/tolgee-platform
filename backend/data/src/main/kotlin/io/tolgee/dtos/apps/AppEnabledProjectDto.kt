package io.tolgee.dtos.apps

data class AppEnabledProjectDto(
  val id: Long,
  val name: String,
  val organizationId: Long,
  val organizationName: String,
  val organizationSlug: String,
)
