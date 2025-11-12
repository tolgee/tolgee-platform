package io.tolgee.fixtures

import io.tolgee.testing.assert
import org.mockito.Mockito

/**
 * Asserts that a specified PostHog event was reported, verifying its structure and data if a check function is provided.
 *
 * @param postHogMock the mocked PostHog instance used for verifying invocations
 * @param eventName the name of the event to look for in PostHog's invocations
 * @param checkFn an optional function to perform additional checks on the event's data, executed if the event is found
 * @return the data of the matched PostHog event as a map
 */
fun assertPostHogEventReported(
  postHogMock: Any,
  eventName: String,
  checkFn: ((Map<*, *>) -> Unit)? = null,
): Map<*, *> {
  return waitForNotThrowing(timeout = 10000) {
    val mockingDetails = Mockito.mockingDetails(postHogMock)
    val invocations = mockingDetails.invocations
    val captureInvocation =
      invocations.last {
        it.method.name == "capture" && it.arguments[1] == eventName
      }
    captureInvocation.assert.isNotNull()
    checkFn?.invoke(captureInvocation!!.arguments[2] as Map<*, *>)
    captureInvocation!!.arguments[2] as Map<*, *>
  }
}
