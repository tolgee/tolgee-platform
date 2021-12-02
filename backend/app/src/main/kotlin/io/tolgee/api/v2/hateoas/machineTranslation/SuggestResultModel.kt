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
      "AWS": "This was translated by AWS"
    }
  """
  )
  val machineTranslations: Map<MtServiceType, String?>?,
  val translationCreditsBalanceBefore: Long,
  val translationCreditsBalanceAfter: Long,
) : RepresentationModel<KeyModel>(), Serializable
