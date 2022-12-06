package io.tolgee.api.v2.hateoas.project

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.language.LanguageModel
import io.tolgee.api.v2.hateoas.organization.SimpleOrganizationModel
import io.tolgee.api.v2.hateoas.permission.ComputedPermissionModel
import io.tolgee.api.v2.hateoas.permission.PermissionModel
import io.tolgee.dtos.Avatar
import io.tolgee.dtos.query_results.ProjectStatistics
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
    example = "EDIT"
  )
  val computedPermission: ComputedPermissionModel,
  val stats: ProjectStatistics,
  val languages: List<LanguageModel>
) : RepresentationModel<ProjectWithStatsModel>()
