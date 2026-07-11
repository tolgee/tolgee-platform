package io.tolgee.component.eventListeners

/**
 * Interface for activity / event listeners whose side-effects can be globally
 * suppressed via a [bypass] flag.
 *
 * Used by `TestDataService` to disable downstream side-effects while seeding
 * test data — so test fixtures stay synthetic.
 */
interface BypassableActivityListener {
  var bypass: Boolean
}
