package io.tolgee.ee.component.limitsAndReporting

import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.service.apps.AppsLimitProvider
import io.tolgee.service.apps.CommunityAppsLimitProvider
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class SelfHostedAppsLimitProvider(
  @Lazy
  private val eeSubscriptionServiceImpl: EeSubscriptionServiceImpl,
) : AppsLimitProvider {
  override fun getAppsLimit(organizationId: Long): Long {
    eeSubscriptionServiceImpl.findSubscriptionDto()
      ?: return CommunityAppsLimitProvider.COMMUNITY_APPS_LIMIT
    // Hardcoded until the self-hosted license payload carries an explicit apps limit.
    return LICENSED_APPS_LIMIT
  }

  companion object {
    const val LICENSED_APPS_LIMIT = 10L
  }
}
