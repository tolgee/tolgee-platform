package io.tolgee.dtos.apps

/** A project an app install is enabled for, with the organization owning that project. */
data class AppEnabledProjectDto(
  val id: Long,
  val name: String,
  val organizationId: Long,
  val organizationName: String,
  val organizationSlug: String,
)
