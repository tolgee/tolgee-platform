package io.tolgee.api.v2.hateoas.machineTranslation

import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

@Suppress("unused")
class CreditBalanceModel(
  val creditBalance: Long
) : RepresentationModel<CreditBalanceModel>(), Serializable
