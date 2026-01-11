package io.tolgee.core.concepts.architecture

/**
 * Marker annotation for port interfaces.
 *
 * Ports define operations the application needs from the outside world. Result hierarchies are
 * defined inside the port interface.
 */
@Target(AnnotationTarget.CLASS) @Retention(AnnotationRetention.RUNTIME) annotation class Port
