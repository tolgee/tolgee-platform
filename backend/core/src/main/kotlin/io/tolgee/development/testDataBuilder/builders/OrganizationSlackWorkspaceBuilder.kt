package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace

class OrganizationSlackWorkspaceBuilder(
  val organizationBuilder: OrganizationBuilder,
) : BaseEntityDataBuilder<OrganizationSlackWorkspace, OrganizationSlackWorkspaceBuilder>() {
  override var self: OrganizationSlackWorkspace =
    OrganizationSlackWorkspace().apply { organization = organizationBuilder.self }
}
