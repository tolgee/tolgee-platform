package io.tolgee.service.aiReview

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.service.featureFlags.FeatureFlagService
import org.springframework.stereotype.Component

@Component
class AiReviewAvailability(
  private val featureFlagService: FeatureFlagService,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  fun isVisible(organizationId: Long?): Boolean = featureFlagService.isEnabled(organizationId, FeatureFlag.AI_REVIEW)

  fun isAvailable(organizationId: Long?): Boolean =
    isVisible(organizationId) && enabledFeaturesProvider.isFeatureEnabled(organizationId, Feature.AI_REVIEW)

  fun checkAvailable(organizationId: Long?) {
    featureFlagService.checkEnabled(organizationId, FeatureFlag.AI_REVIEW)
    enabledFeaturesProvider.checkFeatureEnabled(organizationId, Feature.AI_REVIEW)
  }
}
