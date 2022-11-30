package io.tolgee.api.v2.hateoas.project

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.language.LanguageModel
import io.tolgee.api.v2.hateoas.organization.SimpleOrganizationModel
import io.tolgee.api.v2.hateoas.permission.PermissionModel
import io.tolgee.dtos.Avatar
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

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
  val organizationRole: OrganizationRoleType?,
  @Schema(description = "Current user's direct permission", example = "MANAGE")
  val directPermission: PermissionModel?,
  val computedPermission: PermissionModel
) : RepresentationModel<ProjectModel>()
