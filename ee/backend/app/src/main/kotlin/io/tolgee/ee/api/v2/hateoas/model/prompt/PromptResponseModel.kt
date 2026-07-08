package io.tolgee.ee.api.v2.hateoas.model.prompt

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import tools.jackson.databind.JsonNode

@Relation(collectionRelation = "promptResponse", itemRelation = "promptResponses")
data class PromptResponseModel(
  val prompt: String,
  val result: String,
  val parsedJson: JsonNode?,
  val price: Int?,
  val usage: PromptResponseUsageModel?,
) : RepresentationModel<PromptResponseModel>()
