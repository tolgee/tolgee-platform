package io.tolgee.component.enabledFeaturesProvider

import io.tolgee.constants.Feature
import org.springframework.stereotype.Component

@Component
class EnabledFeaturesProviderOssImpl : EnabledFeaturesProvider {
  override fun get(organizationId: Long?): Array<Feature> {
    return emptyArray()
  }
}
