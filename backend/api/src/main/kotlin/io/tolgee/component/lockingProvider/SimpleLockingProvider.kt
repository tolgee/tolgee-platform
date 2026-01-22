package io.tolgee.component.lockingProvider

import io.tolgee.component.LockingProvider
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

open class SimpleLockingProvider :
  LockingProvider,
  Logging {
  companion object {
    val map = ConcurrentHashMap<String, ReentrantLock>()
  }

  override fun getLock(name: String): Lock {
    return map.computeIfAbsent(name) { ReentrantLock() }
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
      lock.unlock()
    }
  }

  override fun <T> withLockingIfFree(
    name: String,
    fn: () -> T,
  ): T? {
    val lock = this.getLock(name) as ReentrantLock
    val acquired = lock.tryLock()
    if (!acquired) {
      logger.debug("Lock '$name' already held, skipping execution")
      return null
    }
    try {
      return fn()
    } finally {
      lock.unlock()
    }
  }

  @Scheduled(fixedRate = 60000)
  fun cleanupUnusedLocks() {
    // Use compute() for atomic removal that doesn't race with computeIfAbsent()
    map.keys.toList().forEach { key ->
      map.compute(key) { _, lock ->
        if (lock != null && !lock.isLocked && !lock.hasQueuedThreads()) {
          null // Remove the lock
        } else {
          lock // Keep the lock
        }
      }
    }
  }
}
