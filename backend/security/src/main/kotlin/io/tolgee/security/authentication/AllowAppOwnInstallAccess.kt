package io.tolgee.security.authentication

/**
 * Admits an app token on an endpoint that reports on the app's **own install** and nothing else.
 *
 * The endpoint must derive the install from the token's own claims — never from a path, query or
 * body parameter — because this annotation is the one thing standing in for the project-scoped
 * enablement check that [AppAccessInterceptor] otherwise requires. An endpoint taking an install
 * identifier from the request and carrying this annotation would let any app read any install.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class AllowAppOwnInstallAccess
