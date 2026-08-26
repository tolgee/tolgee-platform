package io.tolgee.model.enums

// Cumulative so OWNER ⊇ MAINTAINER ⊇ MEMBER. File-level vals (not companion) to avoid the enum
// constructor referencing an uninitialised companion.
private val ORG_MEMBER_SCOPES =
  arrayOf(
    Scope.ORGANIZATION_MEMBERS_VIEW,
    Scope.ORGANIZATION_USAGE_VIEW,
    Scope.ORGANIZATION_TRANSLATION_MEMORY_VIEW,
  )

private val ORG_MAINTAINER_SCOPES =
  ORG_MEMBER_SCOPES +
    arrayOf(
      Scope.ORGANIZATION_PROJECTS_CREATE,
      Scope.ORGANIZATION_GLOSSARIES_MANAGE,
      Scope.ORGANIZATION_GLOSSARY_TERMS_MANAGE,
      Scope.ORGANIZATION_TRANSLATION_MEMORY_MANAGE,
      Scope.ORGANIZATION_TRANSLATION_MEMORY_ENTRIES_MANAGE,
    )

private val ORG_OWNER_SCOPES =
  ORG_MAINTAINER_SCOPES +
    arrayOf(
      Scope.ORGANIZATION_MEMBERS_MANAGE,
      Scope.ORGANIZATION_SETTINGS_MANAGE,
      Scope.ORGANIZATION_DELETE,
      Scope.ORGANIZATION_APPS_MANAGE,
      Scope.ORGANIZATION_SLACK_MANAGE,
      Scope.ORGANIZATION_AI_MANAGE,
      Scope.ORGANIZATION_BILLING_VIEW,
      Scope.ORGANIZATION_BILLING_MANAGE,
    )

/**
 * Ordinal-persisted by [io.tolgee.model.OrganizationRole.type] — do not reorder.
 *
 * Each level maps to a set of organization-level [Scope]s ([availableScopes]), the way
 * [ProjectPermissionType] maps to project scopes. Enforcement checks these scopes; the level is kept
 * only for storage and the UI.
 */
enum class OrganizationRoleType(
  val isReadOnly: Boolean,
  val availableScopes: Array<Scope>,
) {
  MEMBER(true, ORG_MEMBER_SCOPES),
  OWNER(false, ORG_OWNER_SCOPES),
  MAINTAINER(false, ORG_MAINTAINER_SCOPES),
}
