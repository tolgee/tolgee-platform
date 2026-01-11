package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.SsoTenant

class SsoTenantBuilder(
  val organizationBuilder: OrganizationBuilder,
) : EntityDataBuilder<SsoTenant, SsoTenantBuilder> {
  override var self: SsoTenant =
    SsoTenant().apply {
      organization = organizationBuilder.self
      organizationBuilder.self.ssoTenant = this
    }
}
