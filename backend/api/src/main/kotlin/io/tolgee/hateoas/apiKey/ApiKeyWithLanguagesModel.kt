package io.tolgee.hateoas.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "apiKeys", itemRelation = "apiKey")
open class ApiKeyWithLanguagesModel(
  @Schema(hidden = true)
  apiKeyModel: ApiKeyModel,
  @Schema(
    description = """Languages for which user has translate permission.""",
    deprecated = true,
  )
  val permittedLanguageIds: Set<Long>?,
) : RepresentationModel<ApiKeyWithLanguagesModel>(),
  IApiKeyModel by apiKeyModel
