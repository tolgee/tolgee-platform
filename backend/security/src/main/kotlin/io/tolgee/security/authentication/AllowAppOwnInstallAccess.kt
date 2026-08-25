package io.tolgee.security.authentication

/**
 * Admits an install-context token on an endpoint that reports on the app's own install only. The
 * endpoint must derive the install from the token's claims, never from a request parameter — this
 * annotation stands in for the project-scoped enablement check, so taking an install id from the
 * request would let any app read any install.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class AllowAppOwnInstallAccess
