package io.tolgee.ee.component

import io.tolgee.component.enabledFeaturesProvider.GrantAwareEnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.service.organization.OrganizationFeatureGrantService
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
  organizationFeatureGrantService: OrganizationFeatureGrantService,
) : GrantAwareEnabledFeaturesProvider(organizationFeatureGrantService) {
  var forceEnabled: Set<Feature>? = null

  override fun getPlanFeatures(organizationId: Long?): Array<Feature> {
    return forceEnabled?.toTypedArray()
      ?: eeSubscriptionService.findSubscriptionEntity()?.enabledFeatures
      ?: emptyArray()
  }
}
