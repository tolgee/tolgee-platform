package io.tolgee.api.v2.hateoas.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "apiKeys", itemRelation = "apiKey")
open class ApiKeyModel(
  @Schema(description = "ID of the API key")
  val id: Long,
  @Schema(description = "Resulting user's api key")
  var key: String = "",
  @Schema(description = "Username of user owner")
  var username: String? = null,
  @Schema(description = "Full name of user owner")
  var userFullName: String? = null,
  @Schema(description = "Api key's project ID")
  var projectId: Long = 0,
  @Schema(description = "Api key's project name")
  var projectName: String = "",
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
) : RepresentationModel<ApiKeyModel>(), Serializable
