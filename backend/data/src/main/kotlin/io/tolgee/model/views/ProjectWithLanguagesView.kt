package io.tolgee.model.views

import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

open class ProjectWithLanguagesView(
  override val id: Long,
  override val name: String,
  override val description: String?,
  override val slug: String?,
  override val avatarHash: String?,
  override val userOwner: UserAccount?,
  override val baseLanguage: Language?,
  override val organizationOwner: Organization?,
  override val organizationRole: OrganizationRoleType?,
  override val directPermissions: Permission.ProjectPermissionType?,
  val permittedLanguageIds: List<Long>?
) : ProjectView {
  companion object {
    fun fromProjectView(view: ProjectView, permittedLanguageIds: List<Long>?): ProjectWithLanguagesView {
      return ProjectWithLanguagesView(
        id = view.id,
        name = view.name,
        description = view.description,
        slug = view.slug,
        avatarHash = view.avatarHash,
        userOwner = view.userOwner,
        baseLanguage = view.baseLanguage,
        organizationOwner = view.organizationOwner,
        organizationRole = view.organizationRole,
        directPermissions = view.directPermissions,
        permittedLanguageIds = permittedLanguageIds
      )
    }
  }
}
