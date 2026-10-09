package io.tolgee.component.reporting

/**
 * PostHog person property names written from more than one place. Attribution reads these by
 * name, so a rename here has to reach every writer at once or the data forks in two.
 */
object PersonProperties {
  const val USER_SOURCE = "userSource"
}
