package io.tolgee.ee.service.eeSubscription

import io.tolgee.constants.Message
import io.tolgee.ee.service.eeSubscription.cloudClient.TolgeeCloudLicencingClient
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.ee.uasge.current.CurrentUsageModel
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class EeSubscriptionUsageService(
  private val client: TolgeeCloudLicencingClient,
  @Lazy
  private val eeSubscriptionService: EeSubscriptionServiceImpl,
  private val catchingService: EeSubscriptionErrorCatchingService,
) {
  fun getUsage(): CurrentUsageModel {
    val eeSubscription =
      eeSubscriptionService.findSubscriptionDto()
        ?: throw NotFoundException(Message.INSTANCE_NOT_USING_LICENSE_KEY)

    return catchingService.catchingLicenseNotFound {
      client.getUsageRemote(eeSubscription.licenseKey)
    }
  }
}
