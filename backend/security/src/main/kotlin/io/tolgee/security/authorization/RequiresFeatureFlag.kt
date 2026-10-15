package io.tolgee.security.authorization

import io.tolgee.constants.FeatureFlag

/**
 * Hides the endpoint (responds with 404) unless all the feature flags are enabled for the organization.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresFeatureFlag(
  vararg val flags: FeatureFlag,
)
