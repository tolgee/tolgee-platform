package io.tolgee.hateoas.llmProvider

import io.tolgee.model.enums.LLMProviderType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "provider", itemRelation = "provider")
open class LLMServerProviderModel(
  var name: String,
  var type: LLMProviderType,
  var priority: String?,
  var apiUrl: String?,
  var model: String?,
  var deployment: String?,
  var keepAlive: String?,
  var format: String?,
) : RepresentationModel<LLMServerProviderModel>()
