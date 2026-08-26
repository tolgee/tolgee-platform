package io.tolgee.security.authorization

import io.tolgee.model.enums.Scope

/**
 * Requires the caller to hold ALL of the given organization-level scopes on the request's
 * organization. The scope-based counterpart of [RequiresOrganizationRole]; both translate to a scope
 * check in `OrganizationAuthorizationInterceptor`.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class RequiresOrganizationScopes(
  val scopes: Array<Scope>,
)
