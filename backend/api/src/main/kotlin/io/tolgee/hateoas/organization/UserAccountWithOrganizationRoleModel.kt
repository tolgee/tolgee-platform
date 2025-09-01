package io.tolgee.hateoas.organization

import io.tolgee.dtos.Avatar
import io.tolgee.hateoas.project.SimpleProjectModel
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "usersInOrganization", itemRelation = "userAccount")
data class UserAccountWithOrganizationRoleModel(
  val id: Long,
  val name: String,
  var username: String,
  val organizationRole: OrganizationRoleType?,
  val projectsWithDirectPermission: List<SimpleProjectModel>,
  val mfaEnabled: Boolean,
  val avatar: Avatar?,
) : RepresentationModel<UserAccountWithOrganizationRoleModel>()
