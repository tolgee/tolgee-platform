package io.tolgee.component.enabledFeaturesProvider

import io.tolgee.constants.Feature
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class PublicEnabledFeaturesProvider() : EnabledFeaturesProvider {
  var forceEnabled: List<Feature>? = null

  override fun get(organizationId: Long): Array<Feature> = forceEnabled?.toTypedArray() ?: emptyArray()
}
