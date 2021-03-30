package io.tolgee.api.v2.hateoas.organization

import io.tolgee.model.Permission
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "organizations", itemRelation = "organization")
open class OrganizationModel(
        val id: Long,
        val name: String,
        val addressPart: String,
        val description: String,
        val basePermission: Permission.RepositoryPermissionType
) : RepresentationModel<OrganizationModel>()
