package io.tolgee.ee.api.v2.hateoas.eeSubscription

import io.tolgee.ee.model.EeSubscription
import io.tolgee.service.security.UserAccountService
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class EeSubscriptionModelAssembler(
  private val userAccountService: UserAccountService,
) : RepresentationModelAssembler<EeSubscription, EeSubscriptionModel> {
  override fun toModel(eeSubscription: EeSubscription): EeSubscriptionModel {
    val currentUserCount = userAccountService.countAll()

    return EeSubscriptionModel(
      name = eeSubscription.name,
      licenseKey = eeSubscription.licenseKey,
      enabledFeatures = eeSubscription.enabledFeatures,
      currentPeriodEnd = eeSubscription.currentPeriodEnd?.time,
      cancelAtPeriodEnd = eeSubscription.cancelAtPeriodEnd,
      currentUserCount = currentUserCount,
      status = eeSubscription.status,
      lastValidCheck = eeSubscription.lastValidCheck,
    )
  }
}
