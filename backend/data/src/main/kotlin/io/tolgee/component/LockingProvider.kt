package io.tolgee.component

import java.util.concurrent.locks.Lock

interface LockingProvider {
  fun getLock(name: String): Lock

  fun <T> withLocking(
    name: String,
    fn: () -> T,
  ): T
}
