package io.tolgee.hateoas.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.permission.IPermissionModel
import io.tolgee.hateoas.project.SimpleProjectModel
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.model.enums.TranslationProtection
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
  override val suggestManageLanguageIds: Collection<Long>?,
  override var scopes: Array<Scope> = arrayOf(),
  @Schema(
    description =
      "The user's own scopes on the project, not narrowed by the API key or OAuth grant. " +
        "A scope here but not in `scopes` is one the credential lacks, not the user.",
  )
  val userScopes: Array<Scope>,
  @get:Schema(
    description =
      "The user's permission type. This field is null if user has assigned " +
        "granular permissions or if returning API key's permissions",
  )
  override val type: ProjectPermissionType?,
  var project: SimpleProjectModel,
  val suggestionsMode: SuggestionsMode,
  val translationProtection: TranslationProtection,
  @Schema(description = "Id of the user the credential belongs to")
  val userId: Long,
) : RepresentationModel<ApiKeyPermissionsModel>(),
  IPermissionModel
