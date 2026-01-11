package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.OrganizationRole
import io.tolgee.model.enums.OrganizationRoleType

class OrganizationRoleBuilder(
  val organizationBuilder: OrganizationBuilder,
) : EntityDataBuilder<OrganizationRole, OrganizationRoleBuilder> {
  override var self: OrganizationRole =
    OrganizationRole(type = OrganizationRoleType.OWNER).apply {
      organization = organizationBuilder.self
      organizationBuilder.self.memberRoles.add(this)
    }
}
