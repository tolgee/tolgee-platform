package io.tolgee.hateoas.project

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.Avatar
import io.tolgee.dtos.queryResults.ProjectStatistics
import io.tolgee.hateoas.language.LanguageModel
import io.tolgee.hateoas.organization.SimpleOrganizationModel
import io.tolgee.hateoas.permission.ComputedPermissionModel
import io.tolgee.hateoas.permission.PermissionModel
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "projects", itemRelation = "project")
open class ProjectWithStatsModel(
  val id: Long,
  val name: String,
  val description: String?,
  val slug: String?,
  val avatar: Avatar?,
  val organizationOwner: SimpleOrganizationModel?,
  val baseLanguage: LanguageModel?,
  val organizationRole: OrganizationRoleType?,
  @Schema(description = "Current user's direct permission", example = "MANAGE")
  val directPermission: PermissionModel?,
  @Schema(
    description = "Actual current user's permissions on this project. You can not sort data by this column!",
    example = "EDIT",
  )
  val computedPermission: ComputedPermissionModel,
  val stats: ProjectStatistics,
  val languages: List<LanguageModel>,
  @Schema(description = "Whether to disable ICU placeholder visualization in the editor and it's support.")
  var icuPlaceholders: Boolean,
) : RepresentationModel<ProjectWithStatsModel>()
