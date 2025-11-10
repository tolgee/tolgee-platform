package io.tolgee.hateoas.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.permission.IPermissionModel
import io.tolgee.hateoas.project.SimpleProjectModel
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "permissions", itemRelation = "permissions")
class ApiKeyPermissionsModel(
  @Schema(description = """The API key's project id or the one provided as query param""")
  val projectId: Long,
  override var viewLanguageIds: Set<Long>?,
  override val translateLanguageIds: Set<Long>?,
  override var stateChangeLanguageIds: Set<Long>?,
  override val suggestLanguageIds: Collection<Long>?,
  override var scopes: Array<Scope> = arrayOf(),
  @get:Schema(
    description =
      "The user's permission type. This field is null if user has assigned " +
        "granular permissions or if returning API key's permissions",
  )
  override val type: ProjectPermissionType?,
  var project: SimpleProjectModel,
) : RepresentationModel<ApiKeyPermissionsModel>(),
  IPermissionModel
