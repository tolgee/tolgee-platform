package io.tolgee.component

import java.util.concurrent.locks.Lock

interface LockingProvider {
  fun getLock(name: String): Lock

  fun withLocking(name: String, fn: () -> Unit) {
    val lock = this.getLock(name)
    lock.lock()
    try {
      fn()
    } finally {
      lock.unlock()
    }
  }
}
