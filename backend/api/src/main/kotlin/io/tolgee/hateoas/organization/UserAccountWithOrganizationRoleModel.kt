package io.tolgee.hateoas.organization

import io.tolgee.dtos.Avatar
import io.tolgee.hateoas.project.SimpleProjectModel
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/**
 * Discloses the user's e-mail (`username`). Allowlisted in `UsernameDisclosureGuardTest`. Served
 * only by the org members list (`GET /v2/organizations/{id}/users`, org-role gated). Do not return
 * this model from a less-gated endpoint — that would leak the e-mail.
 */
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
