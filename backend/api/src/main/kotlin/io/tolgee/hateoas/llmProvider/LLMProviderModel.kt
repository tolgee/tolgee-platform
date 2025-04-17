package io.tolgee.hateoas.llmProvider

import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.model.enums.LLMProviderType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "providers", itemRelation = "provider")
open class LLMProviderModel(
  var id: Long,
  var name: String,
  var type: LLMProviderType,
  var priority: LLMProviderPriority?,
  var apiKey: String?,
  var apiUrl: String?,
  var model: String?,
  var deployment: String?,
  var keepAlive: String?,
  var format: String?,
) : RepresentationModel<LLMProviderModel>()
