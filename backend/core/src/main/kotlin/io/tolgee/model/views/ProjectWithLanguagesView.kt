package io.tolgee.model.views

import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.key.Namespace

open class ProjectWithLanguagesView(
  override val id: Long,
  override val name: String,
  override val description: String?,
  override val slug: String?,
  override val avatarHash: String?,
  override val useNamespaces: Boolean,
  override val defaultNamespace: Namespace?,
  override val organizationOwner: Organization,
  override val organizationRole: OrganizationRoleType?,
  override val directPermission: Permission?,
  override var icuPlaceholders: Boolean,
  override var suggestionsMode: SuggestionsMode,
  override var translationProtection: TranslationProtection,
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
        useNamespaces = view.useNamespaces,
        defaultNamespace = view.defaultNamespace,
        organizationOwner = view.organizationOwner,
        organizationRole = view.organizationRole,
        directPermission = view.directPermission,
        permittedLanguageIds = permittedLanguageIds,
        icuPlaceholders = view.icuPlaceholders,
        suggestionsMode = view.suggestionsMode,
        translationProtection = view.translationProtection,
      )
    }
  }
}
