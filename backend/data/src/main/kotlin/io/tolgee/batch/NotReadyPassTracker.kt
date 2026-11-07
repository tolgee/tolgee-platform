package io.tolgee.batch

class NotReadyPassTracker {
  private var notReadyPulled = 0

  fun shouldPollAgainImmediately(
    somethingHandled: Boolean,
    pulled: Int,
    queuedJobs: Int,
  ): Boolean {
    if (somethingHandled) {
      notReadyPulled = 0
      return true
    }
    notReadyPulled += pulled
    if (pulled == 0 || notReadyPulled >= queuedJobs) {
      notReadyPulled = 0
      return false
    }
    return true
  }
}
