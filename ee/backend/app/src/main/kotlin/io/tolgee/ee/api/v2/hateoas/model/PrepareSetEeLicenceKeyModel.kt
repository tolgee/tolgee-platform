package io.tolgee.ee.api.v2.hateoas.model

import io.tolgee.ee.api.v2.hateoas.model.uasge.UsageModel
import org.springframework.hateoas.RepresentationModel

@Suppress("unused")
class PrepareSetEeLicenceKeyModel() : RepresentationModel<PrepareSetEeLicenceKeyModel>() {
  lateinit var plan: SelfHostedEePlanModel
  lateinit var usage: UsageModel
}
