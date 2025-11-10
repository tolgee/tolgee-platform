package io.tolgee.hateoas.aiPtomptCustomization

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.language.LanguageModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "promptCustomizations", itemRelation = "promptCustomization")
open class LanguageAiPromptCustomizationModel(
  @Schema(
    description =
      "The language description used in the  prompt that " +
        "helps AI translator to fine tune results for specific language",
    example =
      "For arabic language, we are super formal. Always use these translations: \n" +
        "Paper -> ورقة\n" +
        "Office -> مكتب\n",
  )
  val description: String?,
  val language: LanguageModel,
) : RepresentationModel<LanguageAiPromptCustomizationModel>(),
  Serializable
