package io.tolgee.api.v2.hateoas.organization

import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "organizations", itemRelation = "organization")
open class OrganizationWithCurrentUserRoleModel(
        val id: Long,
        val name: String,
        val addressPart: String,
        val description: String?,
        val basePermission: Permission.RepositoryPermissionType,
        val currentUserRole: OrganizationRoleType?
) : RepresentationModel<OrganizationWithCurrentUserRoleModel>()
