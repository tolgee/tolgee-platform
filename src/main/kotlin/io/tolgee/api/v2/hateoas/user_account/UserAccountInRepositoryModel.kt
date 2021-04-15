package io.tolgee.api.v2.hateoas.user_account

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.views.UserAccountInRepositoryView
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "users", itemRelation = "user")
data class UserAccountInRepositoryModel(
        override val id: Long,
        override val username: String,
        override var name: String,
        override val organizationRole: OrganizationRoleType?,
        override val organizationBasePermissions: Permission.RepositoryPermissionType?,
        override val directPermissions: Permission.RepositoryPermissionType?,
        @Schema(description = "Actual user's permissions on selected repository. You can not sort data by this column!", example = "EDIT")
        val computedPermissions: Permission.RepositoryPermissionType
) : RepresentationModel<UserAccountInRepositoryModel>(), UserAccountInRepositoryView

