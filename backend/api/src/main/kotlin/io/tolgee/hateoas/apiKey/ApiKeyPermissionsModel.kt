package io.tolgee.hateoas.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "permissions", itemRelation = "permissions")
open class ApiKeyPermissionsModel(
  projectId: Long,

  @Schema(description = """Languages user can translate to""")
  val translateLanguageIds: Set<Long>?,

  @Schema(deprecated = true, description = "Languages user can view")
  var viewLanguages: Set<Long>?,

  @Schema(deprecated = true, description = "Languages user can change translation state (review)")
  var stateChangeLanguages: Set<Long>?,

  @Schema(
    description = "Api key's permission scopes",
    example =
    """
      [
        "screenshots.upload",
        "screenshots.delete",
        "translations.edit", 
        "screenshots.view", 
        "translations.view", 
        "keys.edit"
        ]
  """
  )
  var scopes: Set<String> = setOf()
) : RepresentationModel<ApiKeyPermissionsModel>()
