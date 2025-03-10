package io.tolgee.hateoas.llmProvider

import io.tolgee.model.enums.LLMProviderType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "provider", itemRelation = "provider")
open class LLMProviderSimpleModel(
  var name: String,
  var source: String?,
  var type: LLMProviderType,
) : RepresentationModel<LLMProviderModel>()
