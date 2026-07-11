package io.tolgee.ee.stubs

import io.tolgee.api.EeSubscriptionDto
import io.tolgee.component.HttpClient
import io.tolgee.ee.EeProperties
import io.tolgee.ee.service.eeSubscription.cloudClient.TolgeeCloudLicencingClient
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class TolgeeCloudLicencingClientStub(
  httpClient: HttpClient,
  eeProperties: EeProperties,
) : TolgeeCloudLicencingClient(httpClient, eeProperties) {
  var enableReporting = false

  override fun reportUsageRemote(
    subscription: EeSubscriptionDto,
    keys: Long?,
    seats: Long?,
  ) {
    // no-op reporting unless told otherwise
    if (enableReporting) {
      super.reportUsageRemote(subscription, keys, seats)
    }
  }
}
