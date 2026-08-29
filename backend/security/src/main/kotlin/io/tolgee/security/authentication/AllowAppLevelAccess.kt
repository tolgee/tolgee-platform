package io.tolgee.security.authentication

/**
 * Admits an **app-level** token (minted from client credentials without an install id) on an endpoint
 * that operates on the app itself rather than any install or project — installation discovery.
 *
 * The endpoint must read only the app identity from the token ([AppAuthentication.appId]); an
 * app-level token binds no project and holds no scopes, so it can reach nothing else.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class AllowAppLevelAccess
