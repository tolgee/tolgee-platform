package io.tolgee.hateoas.llmProvider

import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "providers", itemRelation = "provider")
open class LlmProviderModel(
  var id: Long,
  var name: String,
  var type: LlmProviderType,
  var priority: LlmProviderPriority?,
  var apiKey: String?,
  var apiUrl: String?,
  var model: String?,
  var deployment: String?,
  var format: String?,
  var reasoningEffort: String?,
) : RepresentationModel<LlmProviderModel>()
