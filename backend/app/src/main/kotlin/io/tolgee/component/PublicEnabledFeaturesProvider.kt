package io.tolgee.component

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.service.EeSubscriptionService
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class PublicEnabledFeaturesProvider(
  private val eeSubscriptionService: EeSubscriptionService
) : EnabledFeaturesProvider {
  var forceEnabled: List<Feature>? = null

  override fun get(organizationId: Long): Array<Feature> =
    forceEnabled?.toTypedArray() ?: eeSubscriptionService.findSubscriptionEntity()?.enabledFeatures ?: emptyArray()
}
