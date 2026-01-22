package io.tolgee.component

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
   */
  fun <T> withLockingIfFree(
    name: String,
    fn: () -> T,
  ): T?
}
