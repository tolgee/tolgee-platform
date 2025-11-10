package io.tolgee.hateoas.aiPtomptCustomization

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "promptCustomizations", itemRelation = "promptCustomization")
open class ProjectAiPromptCustomizationModel(
  @Schema(
    description =
      "The project description used in the  prompt that " +
        "helps AI translator to understand the context of your project.",
    example =
      "We are Dunder Mifflin, a paper company. We sell paper. " +
        "This is an project of translations for out paper selling app.",
  )
  val description: String?,
) : RepresentationModel<ProjectAiPromptCustomizationModel>(),
  Serializable
