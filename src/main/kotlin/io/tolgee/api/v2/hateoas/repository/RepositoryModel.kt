package io.tolgee.api.v2.hateoas.repository

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.user_account.UserAccountModel
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "repositories", itemRelation = "repository")
open class RepositoryModel(
      val id: Long,
      val name: String,
      val description: String?,
      val addressPart: String?,
      val userOwner: UserAccountModel?,
      val organizationOwnerName: String?,
      val organizationOwnerAddressPart: String?,
      val organizationOwnerBasePermissions: Permission.RepositoryPermissionType?,
      val organizationRole: OrganizationRoleType?,
      @Schema(description = "Current user's direct permission", example = "MANAGE")
      val directPermissions: Permission.RepositoryPermissionType?,
      @Schema(description = "Actual current user's permissions on this repository. You can not sort data by this column!", example = "EDIT")
      val computedPermissions: Permission.RepositoryPermissionType?
) : RepresentationModel<RepositoryModel>()
