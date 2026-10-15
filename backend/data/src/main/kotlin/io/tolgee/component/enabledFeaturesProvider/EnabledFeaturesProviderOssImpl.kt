package io.tolgee.component.enabledFeaturesProvider

import io.tolgee.constants.Feature
import io.tolgee.service.featureFlags.OrganizationFeatureGrantService
import org.springframework.stereotype.Component

@Component
class EnabledFeaturesProviderOssImpl(
  private val organizationFeatureGrantService: OrganizationFeatureGrantService,
) : EnabledFeaturesProvider {
  override fun get(organizationId: Long?): Array<Feature> {
    return organizationFeatureGrantService.getGrantedPlanFeatures(organizationId).toTypedArray()
  }
}
