package io.tolgee.security.authentication

/**
 * Refuses app tokens on an endpoint that would otherwise be reachable because it is project-scoped.
 *
 * For endpoints that administer apps themselves: they are gated on the caller's project scopes, but
 * an app holding those scopes could use them to grant itself another install's — so an app is never
 * allowed to manage apps.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DenyAppAccess
