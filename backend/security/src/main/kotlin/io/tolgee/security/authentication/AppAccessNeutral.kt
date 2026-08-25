package io.tolgee.security.authentication

/**
 * Marks a `/v2/public/apps` endpoint that authenticates from the request body, so [AppAccessInterceptor]
 * must ignore any bearer app token the caller also sends (the normal SDK pattern of a default
 * `Authorization` header would otherwise 403 it). The controller still authenticates the body.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class AppAccessNeutral
