package io.tolgee.api.v2.hateoas.organization

import io.tolgee.api.v2.hateoas.project.SimpleProjectModel
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "usersInOrganization", itemRelation = "userAccount")
data class UserAccountWithOrganizationRoleModel(
  val id: Long,
  val name: String,
  var username: String,
  var organizationRole: OrganizationRoleType?,
  var projectsWithDirectPermission: List<SimpleProjectModel>
) : RepresentationModel<UserAccountWithOrganizationRoleModel>()
