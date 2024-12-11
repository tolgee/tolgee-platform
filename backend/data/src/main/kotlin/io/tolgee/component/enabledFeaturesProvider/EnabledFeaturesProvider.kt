package io.tolgee.component.enabledFeaturesProvider

import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

interface EnabledFeaturesProvider {
  fun get(organizationId: Long?): Array<Feature>

  fun isFeatureEnabled(
    organizationId: Long?,
    feature: Feature,
  ): Boolean {
    return this.get(organizationId).contains(feature)
  }

  fun checkFeatureEnabled(
    organizationId: Long?,
    feature: Feature,
  ) {
    if (!this.isFeatureEnabled(organizationId, feature)) {
      throw BadRequestException(Message.FEATURE_NOT_ENABLED, listOf(feature))
    }
  }

  fun checkOneOfFeaturesEnabled(
    organizationId: Long?,
    features: Collection<Feature>,
  ) {
    if (features.find { this.isFeatureEnabled(organizationId, it) } == null) {
      throw BadRequestException(Message.FEATURE_NOT_ENABLED, features.toList())
    }
  }
}
