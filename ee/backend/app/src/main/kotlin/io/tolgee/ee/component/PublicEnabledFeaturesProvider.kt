package io.tolgee.ee.component

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Qualifier("publicEnabledFeaturesProvider")
@Primary
@ConditionalOnMissingBean(name = ["billingEnabledFeaturesProvider"])
class PublicEnabledFeaturesProvider(
  private val eeSubscriptionService: EeSubscriptionServiceImpl,
) : EnabledFeaturesProvider {
  var forceEnabled: Set<Feature>? = null

  override fun get(organizationId: Long?): Array<Feature> {
    val fromPlan =
      forceEnabled?.toTypedArray() ?: eeSubscriptionService.findSubscriptionEntity()?.enabledFeatures ?: emptyArray()
    val granted = eeSubscriptionService.getGrantedFeatures(organizationId)
    if (granted.isEmpty()) {
      return fromPlan
    }
    return (fromPlan.toList() + granted).toSet().toTypedArray()
  }
}
