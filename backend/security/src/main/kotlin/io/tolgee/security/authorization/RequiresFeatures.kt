package io.tolgee.security.authorization

import io.tolgee.constants.Feature

/**
 * Annotation to specify that a method requires specific features to be enabled for the organization.
 * The organization ID is obtained from the OrganizationHolder or from the project's organization owner.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresFeatures(
  vararg val features: Feature,
)
