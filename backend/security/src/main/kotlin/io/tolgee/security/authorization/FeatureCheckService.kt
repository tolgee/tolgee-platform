package io.tolgee.security.authorization

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import org.springframework.stereotype.Component

@Component
class FeatureCheckService(
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  fun checkFeaturesEnabled(
    organizationId: Long,
    features: Array<out Feature>,
  ) {
    val missing = features.filter { !enabledFeaturesProvider.isFeatureEnabled(organizationId, it) }
    if (missing.isNotEmpty()) {
      throw BadRequestException(Message.FEATURE_NOT_ENABLED, missing)
    }
  }

  fun checkOneOfFeaturesEnabled(
    organizationId: Long,
    features: Array<out Feature>,
  ) {
    val anyEnabled = features.any { enabledFeaturesProvider.isFeatureEnabled(organizationId, it) }
    if (!anyEnabled) {
      throw BadRequestException(Message.FEATURE_NOT_ENABLED, features.toList())
    }
  }
}
