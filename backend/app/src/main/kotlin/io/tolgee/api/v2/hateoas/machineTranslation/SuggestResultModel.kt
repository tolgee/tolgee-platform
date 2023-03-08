package io.tolgee.api.v2.hateoas.machineTranslation

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.key.KeyModel
import io.tolgee.constants.MtServiceType
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

@Suppress("unused")
class SuggestResultModel(
  @Schema(
    description = "Results provided by enabled services",
    example = """
    {
      "GOOGLE": "This was translated by Google",
      "AWS": "This was translated by AWS",
      "DEEPL": "This was translated by DeepL",
      "AZURE": "This was translated by Azure Cognitive",
      "BAIDU": "This was translated by Baidu"
    }
  """
  )
  val machineTranslations: Map<MtServiceType, String?>?,
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
