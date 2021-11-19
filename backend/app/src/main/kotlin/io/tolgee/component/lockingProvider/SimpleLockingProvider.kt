package io.tolgee.component.lockingProvider

import java.util.*
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
}
