package io.tolgee.component.enabledFeaturesProvider

import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException

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
    checkFeaturesEnabled(organizationId, listOf(feature))
  }

  fun checkFeaturesEnabled(
    organizationId: Long?,
    features: Collection<Feature>,
  ) {
    val missing = features.filter { !this.isFeatureEnabled(organizationId, it) }
    if (missing.isNotEmpty()) {
      throwFeaturesMissing(missing)
    }
  }

  fun checkOneOfFeaturesEnabled(
    organizationId: Long?,
    features: Collection<Feature>,
  ) {
    if (features.none { this.isFeatureEnabled(organizationId, it) }) {
      throwFeaturesMissing(features)
    }
  }

  private fun throwFeaturesMissing(missing: Collection<Feature>): Nothing {
    val visible = missing.filter { !it.hidden }
    if (visible.isEmpty()) {
      throw NotFoundException()
    }
    throw BadRequestException(Message.FEATURE_NOT_ENABLED, visible)
  }
}
