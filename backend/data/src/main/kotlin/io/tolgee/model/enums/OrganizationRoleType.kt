package io.tolgee.model.enums

/** Ordinal-persisted by [io.tolgee.model.OrganizationRole.type] — do not reorder. */
enum class OrganizationRoleType(
  val isReadOnly: Boolean,
) {
  MEMBER(true),
  OWNER(false),
  MAINTAINER(false),
}
