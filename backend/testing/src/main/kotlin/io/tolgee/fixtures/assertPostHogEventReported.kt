package io.tolgee.fixtures

import org.mockito.Mockito
import org.mockito.invocation.Invocation

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
    val data = postHogCaptures(postHogMock, eventName).last().arguments[2] as Map<*, *>
    checkFn?.invoke(data)
    data
  }
}

/**
 * Captures reach the mock on the async reporter, so this is a snapshot: only meaningful after
 * [assertPostHogEventReported] (or another wait) has seen the latest event you expect.
 */
fun postHogEventsCapturedSoFar(
  postHogMock: Any,
  eventName: String,
): Int = postHogCaptures(postHogMock, eventName).size

private fun postHogCaptures(
  postHogMock: Any,
  eventName: String,
): List<Invocation> =
  Mockito.mockingDetails(postHogMock).invocations.filter {
    it.method.name == "capture" && it.arguments[1] == eventName
  }
