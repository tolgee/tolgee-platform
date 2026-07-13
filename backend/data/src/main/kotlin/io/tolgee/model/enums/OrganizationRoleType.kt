package io.tolgee.model.enums

/**
 * `OrganizationRole.type` persists this enum by ORDINAL — new values must only ever be appended,
 * and declaration order can never change.
 */
enum class OrganizationRoleType(
  val isReadOnly: Boolean,
) {
  MEMBER(true),
  OWNER(false),
  MAINTAINER(false),
}
