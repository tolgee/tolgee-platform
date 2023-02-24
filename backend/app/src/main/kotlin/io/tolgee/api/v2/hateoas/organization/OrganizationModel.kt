package io.tolgee.api.v2.hateoas.organization

import io.tolgee.api.v2.hateoas.permission.PermissionModel
import io.tolgee.dtos.Avatar
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "organizations", itemRelation = "organization")
open class OrganizationModel(
  override val id: Long,

  override val name: String,

  override val slug: String,

  override val description: String?,

  override val basePermission: PermissionModel,

  override val currentUserRole: OrganizationRoleType?,

  override var avatar: Avatar?,
) : RepresentationModel<OrganizationModel>(), IOrganizationModel
