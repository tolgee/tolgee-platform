package io.tolgee.hateoas.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "apiKeys", itemRelation = "apiKey")
open class RevealedApiKeyModel(
  @Schema(hidden = true)
  apiKeyModel: ApiKeyModel,
  @Schema(description = "Resulting user's api key")
  var key: String = "",
) : RepresentationModel<RevealedApiKeyModel>(),
  Serializable,
  IApiKeyModel by apiKeyModel
