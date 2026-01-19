package io.tolgee.component.lockingProvider

import io.tolgee.component.LockingProvider
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

open class SimpleLockingProvider : LockingProvider {
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

  @Scheduled(fixedRate = 60000)
  fun cleanupUnusedLocks() {
    map.entries.removeIf { !it.value.isLocked }
  }
}
