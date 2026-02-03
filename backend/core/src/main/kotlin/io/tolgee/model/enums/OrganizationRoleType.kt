package io.tolgee.model.enums

enum class OrganizationRoleType(
  val isReadOnly: Boolean,
) {
  MEMBER(true),
  OWNER(false),
  MAINTAINER(false),
}
