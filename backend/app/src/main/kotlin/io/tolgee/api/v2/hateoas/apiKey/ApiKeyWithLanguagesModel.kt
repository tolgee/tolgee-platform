package io.tolgee.api.v2.hateoas.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "apiKeys", itemRelation = "apiKey")
open class ApiKeyWithLanguagesModel(
  id: Long,
  key: String,
  username: String?,
  userFullName: String?,
  projectId: Long,
  projectName: String,
  scopes: Set<String>,

  @Schema(
    description = """Languages for which user has translate permission.

If null, all languages are permitted.
  """
  )
  val permittedLanguageIds: List<Long>?
) : ApiKeyModel(id, key, username, userFullName, projectId, projectName, scopes)
