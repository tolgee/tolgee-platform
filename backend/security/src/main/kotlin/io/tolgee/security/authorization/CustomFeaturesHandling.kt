package io.tolgee.security.authorization

/**
 * Annotation to specify that a method implements custom features handling logic.
 * This is used to mark methods that directly interface with the features provider
 * to implement their own authorization logic.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomFeaturesHandling