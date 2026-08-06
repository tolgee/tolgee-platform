package io.tolgee.hateoas.llmProvider

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.LlmProviderType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "providers", itemRelation = "provider")
open class LlmProviderSimpleModel(
  var name: String,
  var source: String?,
  var type: LlmProviderType,
  var tokenPriceInCreditsInput: Double?,
  var tokenPriceInCreditsOutput: Double?,
  @field:Schema(
    description =
      "Name of the concrete provider the server default (\"default\" provider) currently resolves to. " +
        "It is only set on the synthetic \"default\" entry and is always null for concrete providers. " +
        "Clients can rely on non-null resolvesToName to identify the server-default entry.",
  )
  var resolvesToName: String? = null,
) : RepresentationModel<LlmProviderModel>()
