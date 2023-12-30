package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.OrganizationRole

class OrganizationRoleBuilder(
  val organizationBuilder: OrganizationBuilder,
) : EntityDataBuilder<OrganizationRole, OrganizationRoleBuilder> {
  override var self: OrganizationRole =
    OrganizationRole().apply {
      organization = organizationBuilder.self
      organizationBuilder.self.memberRoles.add(this)
    }
}
