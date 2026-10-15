package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.OrganizationFeatureGrant
import io.tolgee.model.enums.OrganizationFeatureGrantKind

class OrganizationFeatureGrantBuilder(
  val organizationBuilder: OrganizationBuilder,
) : BaseEntityDataBuilder<OrganizationFeatureGrant, OrganizationFeatureGrantBuilder>() {
  override var self: OrganizationFeatureGrant =
    OrganizationFeatureGrant(
      organization = organizationBuilder.self,
      kind = OrganizationFeatureGrantKind.FEATURE_FLAG,
      name = "",
    )
}
