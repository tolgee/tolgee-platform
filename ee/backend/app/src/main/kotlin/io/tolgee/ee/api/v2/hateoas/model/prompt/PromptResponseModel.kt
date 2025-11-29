package io.tolgee.ee.api.v2.hateoas.model.prompt

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "promptResponse", itemRelation = "promptResponses")
data class PromptResponseModel(
  val prompt: String,
  val result: String,
  val parsedJson: JsonNode?,
  val price: Int?,
  val usage: PromptResponseUsageModel?,
) : RepresentationModel<PromptResponseModel>()
