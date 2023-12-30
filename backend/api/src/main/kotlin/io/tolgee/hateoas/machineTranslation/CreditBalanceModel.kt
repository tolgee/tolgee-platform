package io.tolgee.hateoas.machineTranslation

import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

@Suppress("unused")
class CreditBalanceModel(
  val creditBalance: Long,
  val bucketSize: Long,
  val extraCreditBalance: Long,
) : RepresentationModel<CreditBalanceModel>(), Serializable
