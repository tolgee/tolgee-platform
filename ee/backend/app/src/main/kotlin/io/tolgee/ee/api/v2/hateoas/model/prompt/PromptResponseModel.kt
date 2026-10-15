package io.tolgee.ee.api.v2.hateoas.model.prompt

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.aiReview.AiReviewIssueModel
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
  @Schema(description = "Score computed from the returned issues (review prompts only)")
  val reviewScore: Int? = null,
  @Schema(description = "Issues returned by the reviewer (review prompts only)")
  val reviewIssues: List<AiReviewIssueModel>? = null,
  @Schema(description = "Error code when the review output could not be parsed (review prompts only)")
  val reviewError: String? = null,
) : RepresentationModel<PromptResponseModel>()
