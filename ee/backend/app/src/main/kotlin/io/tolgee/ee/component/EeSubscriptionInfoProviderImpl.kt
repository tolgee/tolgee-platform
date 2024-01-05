package io.tolgee.ee.component

import io.tolgee.component.EeSubscriptionInfoProvider
import io.tolgee.ee.service.EeSubscriptionService
import org.springframework.stereotype.Component

@Component
class EeSubscriptionInfoProviderImpl(
  private val eeSubscriptionService: EeSubscriptionService,
) : EeSubscriptionInfoProvider {
  override fun isSubscribed(): Boolean = eeSubscriptionService.isSubscribed()
}
