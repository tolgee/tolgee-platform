package io.tolgee.component.lockingProvider

import org.redisson.api.RLock

/**
 * Redisson commands fail on an interrupted thread. Unlocking with the flag set leaves the lock
 * held until the watchdog lease runs out, and every later holder blocks on it.
 */
fun RLock.releaseEvenIfInterrupted() {
  val interrupted = Thread.interrupted()
  try {
    unlock()
  } catch (e: IllegalMonitorStateException) {
    // not held by this thread, nothing to release
  } finally {
    if (interrupted) {
      Thread.currentThread().interrupt()
    }
  }
}
