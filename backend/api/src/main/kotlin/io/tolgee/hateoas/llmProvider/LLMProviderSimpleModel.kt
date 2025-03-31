package io.tolgee.hateoas.llmProvider

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "provider", itemRelation = "provider")
open class LLMProviderSimpleModel(
  var name: String,
  var source: String?,
) : RepresentationModel<LLMProviderModel>()
