package io.tolgee.model.views

import io.tolgee.dtos.query_results.ProjectStatistics
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class ProjectWithStatsView(
  projectView: ProjectView,
  val stats: ProjectStatistics,
  val languages: List<Language>
) : ProjectView {
  override val id: Long = projectView.id
  override val name: String = projectView.name
  override val description: String? = projectView.description
  override val slug: String? = projectView.slug
  override val avatarHash: String? = projectView.avatarHash
  override val userOwner: UserAccount? = projectView.userOwner
  override val baseLanguage: Language? = projectView.baseLanguage
  override val organizationOwnerName: String? = projectView.organizationOwnerName
  override val organizationOwnerSlug: String? = projectView.organizationOwnerSlug
  override val organizationBasePermissions: Permission.ProjectPermissionType? = projectView.organizationBasePermissions
  override val organizationRole: OrganizationRoleType? = projectView.organizationRole
  override val directPermissions: Permission.ProjectPermissionType? = projectView.directPermissions
}
