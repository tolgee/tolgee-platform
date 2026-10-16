package io.tolgee.batch

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class NotReadyPassTrackerTest {
  private val tracker = NotReadyPassTracker()

  @Test
  fun `keeps polling until a full pass of not ready items was pulled`() {
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 3, queueSize = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 3, queueSize = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 3, queueSize = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 3, queueSize = 10).assert.isFalse()
  }

  @Test
  fun `starts a new pass after pausing`() {
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 5, queueSize = 5).assert.isFalse()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 2, queueSize = 5).assert.isTrue()
  }

  @Test
  fun `pauses when nothing was pulled`() {
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 0, queueSize = 5).assert.isFalse()
  }

  @Test
  fun `a handled item resets the pass and polls again`() {
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 4, queueSize = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = true, pulled = 1, queueSize = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 4, queueSize = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 6, queueSize = 10).assert.isFalse()
  }
}
