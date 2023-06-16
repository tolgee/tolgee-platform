package io.tolgee.hateoas.machineTranslation

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.MtServiceType
import io.tolgee.hateoas.key.KeyModel
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

@Suppress("unused")
class SuggestResultModel(
  @Schema(
    description = "Results provided by enabled services",
    example = """
    {
      "GOOGLE": {
        "output": "This was translated by Google",
        "contextDescription": null
      },
      "TOLGEE": {
        "output": "This was translatied by Tolgee",
        "contextDescription": "This is an example in swagger"
      } 
    }
  """
  )
  val machineTranslations: Map<MtServiceType, TranslationItemModel?>?,
  val translationCreditsBalanceBefore: Long,
  val translationCreditsBalanceAfter: Long,

  @Schema(
    description = "Extra credits are neither refilled nor reset every period." +
      " User's can refill them on Tolgee cloud."
  )
  val translationExtraCreditsBalanceBefore: Long,

  @Schema(
    description = "Extra credits are neither refilled nor reset every period." +
      " User's can refill them on Tolgee cloud."
  )
  val translationExtraCreditsBalanceAfter: Long,
) : RepresentationModel<KeyModel>(), Serializable
