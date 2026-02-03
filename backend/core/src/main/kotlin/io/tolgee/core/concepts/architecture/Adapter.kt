package io.tolgee.core.concepts.architecture

/**
 * Marker annotation for port implementations in external modules.
 *
 * Adapters translate between core and external shapes. They are invisible to core (core never
 * imports adapter classes).
 */
@Target(AnnotationTarget.CLASS) @Retention(AnnotationRetention.RUNTIME) annotation class Adapter
