package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.api.EeSubscriptionDto
import io.tolgee.hateoas.ee.IEeSubscriptionModelAssembler
import io.tolgee.hateoas.ee.eeSubscription.EeSubscriptionModel
import io.tolgee.service.security.UserAccountService
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class EeSubscriptionModelAssembler(
  private val userAccountService: UserAccountService,
) : RepresentationModelAssembler<EeSubscriptionDto, EeSubscriptionModel>,
  IEeSubscriptionModelAssembler {
  override fun toModel(eeSubscription: EeSubscriptionDto): EeSubscriptionModel {
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
      nonCommerical = eeSubscription.nonCommercial,
      isPayAsYouGo = eeSubscription.isPayAsYouGo,
    )
  }
}
