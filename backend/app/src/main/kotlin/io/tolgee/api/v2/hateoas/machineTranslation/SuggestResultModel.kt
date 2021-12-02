package io.tolgee.api.v2.hateoas.machineTranslation

import io.tolgee.api.v2.hateoas.key.KeyModel
import io.tolgee.constants.MachineTranslationServiceType
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

@Suppress("unused")
class SuggestResultModel(
  val machineTranslations: Map<MachineTranslationServiceType, String?>?,
  val translationCreditsBalanceBefore: Long,
  val translationCreditsBalanceAfter: Long,
) : RepresentationModel<KeyModel>(), Serializable
