package io.tolgee.component.enabledFeaturesProvider

import io.tolgee.constants.Feature
import io.tolgee.service.organization.OrganizationFeatureGrantService

abstract class GrantAwareEnabledFeaturesProvider(
  private val organizationFeatureGrantService: OrganizationFeatureGrantService,
) : EnabledFeaturesProvider {
  final override fun get(organizationId: Long?): Array<Feature> {
    return organizationFeatureGrantService.withGrants(organizationId, getPlanFeatures(organizationId))
  }
}
