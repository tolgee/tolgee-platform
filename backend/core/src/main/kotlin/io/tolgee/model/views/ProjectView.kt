package io.tolgee.model.views

import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.key.Namespace

interface ProjectView {
  val id: Long
  val name: String
  val description: String?
  val slug: String?
  val avatarHash: String?
  val useNamespaces: Boolean
  val defaultNamespace: Namespace?
  val organizationOwner: Organization
  val organizationRole: OrganizationRoleType?
  val directPermission: Permission?
  var icuPlaceholders: Boolean
  var suggestionsMode: SuggestionsMode
  var translationProtection: TranslationProtection
}
