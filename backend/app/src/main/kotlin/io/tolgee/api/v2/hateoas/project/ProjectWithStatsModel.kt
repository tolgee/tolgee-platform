package io.tolgee.api.v2.hateoas.project

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.UserPermissionModel
import io.tolgee.api.v2.hateoas.organization.LanguageModel
import io.tolgee.api.v2.hateoas.user_account.UserAccountModel
import io.tolgee.dtos.Avatar
import io.tolgee.dtos.query_results.ProjectStatistics
import io.tolgee.model.Permission
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
  val userOwner: UserAccountModel?,
  val baseLanguage: LanguageModel?,
  val organizationOwnerName: String?,
  val organizationOwnerSlug: String?,
  val organizationOwnerBasePermissions: Permission.ProjectPermissionType?,
  val organizationRole: OrganizationRoleType?,
  @Schema(description = "Current user's direct permission", example = "MANAGE")
  val directPermissions: Permission.ProjectPermissionType?,
  @Schema(
    description = "Actual current user's permissions on this project. You can not sort data by this column!",
    example = "EDIT"
  )
  val computedPermissions: UserPermissionModel,
  val stats: ProjectStatistics,
  val languages: List<LanguageModel>
) : RepresentationModel<ProjectWithStatsModel>()
