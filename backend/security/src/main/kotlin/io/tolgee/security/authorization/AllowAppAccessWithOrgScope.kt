package io.tolgee.security.authorization

import io.tolgee.model.enums.Scope

/**
 * Opts an organization-level endpoint in to access by an installed Tolgee App.
 *
 * Apps are otherwise rejected from org-level endpoints. With this annotation, an app token is
 * allowed when its install belongs to the **target organization** and the install's granted scopes
 * include the required org-level [scopes]. The org owner consents to those scopes at install time,
 * so the install grant alone authorizes the call — no user organization role is required.
 *
 * The [scopes] must be organization-level scopes (see [Scope.organizationLevelScopes]).
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class AllowAppAccessWithOrgScope(
  vararg val scopes: Scope,
)
