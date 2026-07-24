package io.tolgee.hateoas.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "apiKeys", itemRelation = "apiKey")
open class ApiKeyModel(
  @Schema(description = "ID of the API key")
  override val id: Long,
  @Schema(description = "Description")
  override val description: String,
  @Deprecated("A user's username (their e-mail) is only disclosed on the project members list.")
  @Schema(
    deprecated = true,
    description =
      "Deprecated: always empty. A user's username (their e-mail) is disclosed only " +
        "on the project members list, never as an API key's owner reference.",
  )
  override var username: String? = "",
  @Schema(description = "Full name of user owner")
  override var userFullName: String? = null,
  @Schema(description = "Api key's project ID")
  override var projectId: Long = 0,
  @Schema(description = "Api key's project name")
  override var projectName: String = "",
  @Schema(description = "Timestamp of API key expiraion")
  override val expiresAt: Long? = null,
  @Schema(description = "Timestamp of API key last usage")
  override val lastUsedAt: Long? = null,
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
  """,
  )
  override var scopes: Set<String> = setOf(),
) : RepresentationModel<ApiKeyModel>(),
  Serializable,
  IApiKeyModel
