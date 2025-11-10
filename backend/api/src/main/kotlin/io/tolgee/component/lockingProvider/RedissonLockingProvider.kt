package io.tolgee.component.lockingProvider

import io.tolgee.component.LockingProvider
import org.redisson.api.RLock
import org.redisson.api.RedissonClient

class RedissonLockingProvider(
  private val redissonClient: RedissonClient,
) : LockingProvider {
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
}
