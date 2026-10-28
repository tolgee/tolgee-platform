package io.tolgee.security.authentication

/** Lets a credential-less visitor (the guest principal) reach a handler outside the project routes. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AllowGuestAccess
