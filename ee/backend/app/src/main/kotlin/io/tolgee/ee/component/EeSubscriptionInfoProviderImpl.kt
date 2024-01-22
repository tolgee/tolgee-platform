package io.tolgee.ee.component

import io.tolgee.component.EeSubscriptionInfoProvider
import io.tolgee.ee.service.EeSubscriptionServiceImpl
import org.springframework.stereotype.Component

@Component
class EeSubscriptionInfoProviderImpl(
  private val eeSubscriptionService: EeSubscriptionServiceImpl,
) : EeSubscriptionInfoProvider {
  override fun isSubscribed(): Boolean = eeSubscriptionService.isSubscribed()
}
