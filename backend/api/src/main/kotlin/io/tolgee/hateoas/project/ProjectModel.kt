package io.tolgee.hateoas.project

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.Avatar
import io.tolgee.hateoas.key.namespace.NamespaceModel
import io.tolgee.hateoas.language.LanguageModel
import io.tolgee.hateoas.organization.SimpleOrganizationModel
import io.tolgee.hateoas.permission.ComputedPermissionModel
import io.tolgee.hateoas.permission.PermissionModel
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.model.enums.TranslationProtection
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.Date

@Suppress("unused")
@Relation(collectionRelation = "projects", itemRelation = "project")
open class ProjectModel(
  val id: Long,
  val name: String,
  val description: String?,
  val slug: String?,
  val avatar: Avatar?,
  val organizationOwner: SimpleOrganizationModel?,
  val baseLanguage: LanguageModel?,
  val useNamespaces: Boolean,
  val useBranching: Boolean,
  val useQaChecks: Boolean,
  @Schema(description = "Whether the project is public — discoverable and open to community suggestions")
  val public: Boolean,
  val defaultNamespace: NamespaceModel?,
  val organizationRole: OrganizationRoleType?,
  @Schema(description = "Current user's direct permission", example = "MANAGE")
  val directPermission: PermissionModel?,
  val computedPermission: ComputedPermissionModel,
  @Schema(description = "Whether to disable ICU placeholder visualization in the editor and it's support.")
  var icuPlaceholders: Boolean,
  @Schema(description = "Suggestions for translations")
  var suggestionsMode: SuggestionsMode,
  @Schema(description = "Level of protection of translations")
  var translationProtection: TranslationProtection,
  @Schema(description = "Markdown shown on the community page. Null means the project has none.")
  val communityDescription: String?,
  @Schema(description = "Markdown on how to contribute, shown on the community page. Null means the default text.")
  val communityGuidelines: String?,
  @Schema(description = "Whether the owner marked the community page as shared outside Tolgee.")
  val communityShared: Boolean,
  @Schema(description = "Whether the community page tells visitors that the app supports in-context translation.")
  val communityInContext: Boolean,
  @Schema(description = "Link to the app that supports in-context translation, http or https.")
  val communityAppUrl: String?,
  val createdAt: Date?,
) : RepresentationModel<ProjectModel>()
