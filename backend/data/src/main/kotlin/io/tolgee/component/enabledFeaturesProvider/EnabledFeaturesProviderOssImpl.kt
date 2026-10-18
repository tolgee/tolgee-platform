package io.tolgee.component.enabledFeaturesProvider

import io.tolgee.constants.Feature
import io.tolgee.service.organization.OrganizationFeatureGrantService
import org.springframework.stereotype.Component

@Component
class EnabledFeaturesProviderOssImpl(
  organizationFeatureGrantService: OrganizationFeatureGrantService,
) : GrantAwareEnabledFeaturesProvider(organizationFeatureGrantService) {
  override fun getPlanFeatures(organizationId: Long?): Array<Feature> {
    return emptyArray()
  }
}
