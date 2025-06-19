package io.tolgee.security.authorization

/**
 * Annotation to specify that a method explicitly does not require any features to be enabled.
 * This is used to mark methods that can be called regardless of feature enablement.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoFeaturesRequired