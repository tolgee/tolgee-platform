package io.tolgee.hateoas.ee

import io.tolgee.api.EeSubscriptionDto
import io.tolgee.hateoas.ee.eeSubscription.EeSubscriptionModel

interface IEeSubscriptionModelAssembler {
  fun toModel(eeSubscription: EeSubscriptionDto): EeSubscriptionModel
}
