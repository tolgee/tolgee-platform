package io.tolgee.model.views

import io.tolgee.model.Language
import io.tolgee.model.key.Namespace
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType

open class ProjectWithLanguagesView(
  override val id: Long,
  override val name: String,
  override val description: String?,
  override val slug: String?,
  override val avatarHash: String?,
  override val baseLanguage: Language?,
  override val baseNamespace: Namespace?,
  override val organizationOwner: Organization,
  override val organizationRole: OrganizationRoleType?,
  override val directPermission: Permission?,
  override var icuPlaceholders: Boolean,
  val permittedLanguageIds: List<Long>?,
) : ProjectView {
  companion object {
    fun fromProjectView(
      view: ProjectView,
      permittedLanguageIds: List<Long>?,
    ): ProjectWithLanguagesView {
      return ProjectWithLanguagesView(
        id = view.id,
        name = view.name,
        description = view.description,
        slug = view.slug,
        avatarHash = view.avatarHash,
        baseLanguage = view.baseLanguage,
        baseNamespace = view.baseNamespace,
        organizationOwner = view.organizationOwner,
        organizationRole = view.organizationRole,
        directPermission = view.directPermission,
        permittedLanguageIds = permittedLanguageIds,
        icuPlaceholders = view.icuPlaceholders,
      )
    }
  }
}
