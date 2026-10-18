package io.tolgee.development.testDataBuilder.builders

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.OrganizationFeatureGrant

class OrganizationFeatureGrantBuilder(
  val organizationBuilder: OrganizationBuilder,
) : EntityDataBuilder<OrganizationFeatureGrant, OrganizationFeatureGrantBuilder> {
  override var self: OrganizationFeatureGrant =
    OrganizationFeatureGrant(
      organization = organizationBuilder.self,
      feature = Feature.GLOSSARY,
    )
}
