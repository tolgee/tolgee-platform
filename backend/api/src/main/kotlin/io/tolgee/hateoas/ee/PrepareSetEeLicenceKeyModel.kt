package io.tolgee.hateoas.ee

import io.tolgee.hateoas.ee.uasge.proportional.UsageModel
import org.springframework.hateoas.RepresentationModel

@Suppress("unused")
class PrepareSetEeLicenceKeyModel() : RepresentationModel<PrepareSetEeLicenceKeyModel>() {
  lateinit var plan: SelfHostedEePlanModel
  lateinit var usage: UsageModel
}
