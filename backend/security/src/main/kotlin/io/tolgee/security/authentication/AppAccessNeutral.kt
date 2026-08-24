package io.tolgee.security.authentication

/**
 * Marks an endpoint that authenticates itself from the request body (client credentials), so it
 * must be indifferent to any bearer app token the caller also happens to send.
 *
 * The token endpoint, app-secret self-service and install discovery live under `/v2/public/apps`
 * and take their credentials in the body. An app whose HTTP client carries its current access token
 * as a default `Authorization` header — the normal SDK pattern, especially while re-minting before
 * the old token expires — would otherwise be denied here by [AppAccessInterceptor] purely for
 * presenting a valid token. This admits the request regardless of the bearer identity; the
 * controller still authenticates the body.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class AppAccessNeutral
