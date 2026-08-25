package io.tolgee.security.authentication

/**
 * Refuses app tokens on an otherwise-reachable project-scoped endpoint. For app-management endpoints:
 * an app holding the required scope could grant itself another install's, so apps never manage apps.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DenyAppAccess
