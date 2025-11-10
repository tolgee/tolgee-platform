package io.tolgee.component.lockingProvider

import io.tolgee.component.LockingProvider
import java.util.WeakHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class SimpleLockingProvider : LockingProvider {
  companion object {
    val map = WeakHashMap<String, Lock>()
  }

  @Synchronized
  override fun getLock(name: String): Lock {
    return map.getOrPut(name) { ReentrantLock() }
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
}
