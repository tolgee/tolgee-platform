package io.tolgee.batch

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class NotReadyPassTrackerTest {
  private val tracker = NotReadyPassTracker()

  @Test
  fun `keeps polling until as many not ready items as queued jobs were pulled`() {
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 3, queuedJobs = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 3, queuedJobs = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 3, queuedJobs = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 3, queuedJobs = 10).assert.isFalse()
  }

  @Test
  fun `starts a new pass after pausing`() {
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 5, queuedJobs = 5).assert.isFalse()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 2, queuedJobs = 5).assert.isTrue()
  }

  @Test
  fun `pauses when nothing was pulled`() {
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 0, queuedJobs = 5).assert.isFalse()
  }

  @Test
  fun `a handled item resets the pass and polls again`() {
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 4, queuedJobs = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = true, pulled = 1, queuedJobs = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 4, queuedJobs = 10).assert.isTrue()
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 6, queuedJobs = 10).assert.isFalse()
  }

  @Test
  fun `pauses after one pass over a single job with many queued chunks`() {
    tracker.shouldPollAgainImmediately(somethingHandled = false, pulled = 19, queuedJobs = 1).assert.isFalse()
  }
}
