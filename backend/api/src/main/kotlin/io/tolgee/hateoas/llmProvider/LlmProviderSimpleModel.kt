package io.tolgee.hateoas.llmProvider

import io.tolgee.model.enums.LlmProviderType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "providers", itemRelation = "provider")
open class LlmProviderSimpleModel(
  var name: String,
  var source: String?,
  var type: LlmProviderType,
) : RepresentationModel<LlmProviderModel>()
