package io.tolgee.component.lockingProvider

import io.tolgee.component.LockingProvider
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.redisson.api.RLock
import org.redisson.api.RedissonClient

open class RedissonLockingProvider(
  private val redissonClient: RedissonClient,
) : LockingProvider, Logging {
  override fun getLock(name: String): RLock {
    return redissonClient.getLock(name)
  }

  override fun <T> withLocking(
    name: String,
    fn: () -> T,
  ): T {
    val lock = this.getLock(name)
    lock.lock()
    try {
      return fn()
    } finally {
      if (lock.isHeldByCurrentThread) {
        lock.unlock()
      }
    }
  }

  override fun <T> withLockingIfFree(
    name: String,
    fn: () -> T,
  ): T? {
    val lock = this.getLock(name)
    val acquired = lock.tryLock()
    if (!acquired) {
      logger.debug("Lock '$name' already held, skipping execution")
      return null
    }
    try {
      return fn()
    } finally {
      if (lock.isHeldByCurrentThread) {
        lock.unlock()
      }
    }
  }
}
