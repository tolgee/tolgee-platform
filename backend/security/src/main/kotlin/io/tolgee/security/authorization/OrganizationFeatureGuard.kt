package io.tolgee.security.authorization

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import org.springframework.stereotype.Component

@Component
class OrganizationFeatureGuard(
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  fun checkFeaturesEnabled(
    organizationId: Long,
    features: Array<out Feature>,
  ) {
    enabledFeaturesProvider.checkFeaturesEnabled(organizationId, features.toList())
  }

  fun checkOneOfFeaturesEnabled(
    organizationId: Long,
    features: Array<out Feature>,
  ) {
    if (features.isEmpty()) return
    enabledFeaturesProvider.checkOneOfFeaturesEnabled(organizationId, features.toList())
  }
}
