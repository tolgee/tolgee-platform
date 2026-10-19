package io.tolgee.component.enabledFeaturesProvider

import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException

interface EnabledFeaturesProvider {
  /**
   * All features the organization can use: the plan features plus the granted ones.
   */
  fun get(organizationId: Long?): Array<Feature>

  /**
   * Only the features coming from the organization's plan or license, without grants.
   */
  fun getPlanFeatures(organizationId: Long?): Array<Feature>

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
    if (missing.isEmpty()) {
      return
    }
    throwFeaturesMissing(missing)
  }

  fun checkOneOfFeaturesEnabled(
    organizationId: Long?,
    features: Collection<Feature>,
  ) {
    if (features.any { this.isFeatureEnabled(organizationId, it) }) {
      return
    }
    throwFeaturesMissing(features)
  }

  private fun throwFeaturesMissing(missing: Collection<Feature>): Nothing {
    val visible = missing.filter { !it.hidden }
    if (visible.isEmpty()) {
      throw NotFoundException()
    }
    throw BadRequestException(Message.FEATURE_NOT_ENABLED, visible)
  }
}
