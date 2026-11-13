package io.tolgee.security.oauth2

import io.tolgee.component.LockingProvider
import java.time.Duration
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/** A [LockingProvider] that always hands the lock over, so a scheduled job's body runs in the test. */
class AlwaysFreeLockingProvider : LockingProvider {
  override fun getLock(name: String): Lock = ReentrantLock()

  override fun <T> withLocking(
    name: String,
    fn: () -> T,
  ): T = fn()

  override fun <T> withLockingIfFree(
    name: String,
    leaseTime: Duration,
    fn: () -> T,
  ): T = fn()
}
