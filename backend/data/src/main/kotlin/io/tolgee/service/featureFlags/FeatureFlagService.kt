package io.tolgee.service.featureFlags

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FeatureFlag
import io.tolgee.exceptions.NotFoundException
import org.springframework.stereotype.Service

@Service
class FeatureFlagService(
  private val tolgeeProperties: TolgeeProperties,
  private val organizationFeatureGrantService: OrganizationFeatureGrantService,
) {
  fun isEnabled(
    organizationId: Long?,
    flag: FeatureFlag,
  ): Boolean {
    return flag in getEnabledFlags(organizationId)
  }

  fun getEnabledFlags(organizationId: Long?): Set<FeatureFlag> {
    val enabledForAll = getEnabledForAll()
    if (organizationId == null) {
      return enabledForAll
    }
    return enabledForAll + organizationFeatureGrantService.getGrants(organizationId).featureFlags
  }

  fun getEnabledForAll(): Set<FeatureFlag> = tolgeeProperties.featureFlags.enabledForAll.toSet()

  fun checkEnabled(
    organizationId: Long?,
    flag: FeatureFlag,
  ) {
    if (!isEnabled(organizationId, flag)) {
      throw NotFoundException()
    }
  }
}
