package io.tolgee.model.views

import io.tolgee.model.Organization
import io.tolgee.model.enums.OrganizationRoleType

interface OrganizationView {
  val organization: Organization
  val currentUserRole: OrganizationRoleType?

  companion object {
    fun of(
      entity: Organization,
      currentUserRole: OrganizationRoleType?,
    ): OrganizationView {
      return object : OrganizationView {
        override val organization = entity
        override val currentUserRole = currentUserRole
      }
    }
  }
}
