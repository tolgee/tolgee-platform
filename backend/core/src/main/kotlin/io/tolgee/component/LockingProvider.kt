package io.tolgee.component

import java.time.Duration
import java.util.concurrent.locks.Lock

interface LockingProvider {
  fun getLock(name: String): Lock

  fun <T> withLocking(
    name: String,
    fn: () -> T,
  ): T

  /**
   * Executes the given function if the lock is free.
   * If the lock is already held, returns null without executing the function.
   * This is useful for scheduled tasks that should only run on one node.
   *
   * @param name The name of the lock
   * @param leaseTime The maximum time to hold the lock. After this time, the lock is automatically released.
   * @param fn The function to execute while holding the lock
   */
  fun <T> withLockingIfFree(
    name: String,
    leaseTime: Duration,
    fn: () -> T,
  ): T?
}
