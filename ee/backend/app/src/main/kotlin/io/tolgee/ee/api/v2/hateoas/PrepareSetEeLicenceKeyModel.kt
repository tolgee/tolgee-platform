package io.tolgee.ee.api.v2.hateoas

import io.tolgee.ee.api.v2.hateoas.uasge.MeteredUsageModel
import org.springframework.hateoas.RepresentationModel

@Suppress("unused")
class PrepareSetEeLicenceKeyModel() : RepresentationModel<PrepareSetEeLicenceKeyModel>() {
  lateinit var plan: SelfHostedEePlanModel
  lateinit var usage: MeteredUsageModel
}
