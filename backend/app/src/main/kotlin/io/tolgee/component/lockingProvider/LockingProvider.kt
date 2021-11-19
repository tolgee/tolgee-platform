package io.tolgee.component.lockingProvider

import java.util.concurrent.locks.Lock

interface LockingProvider {
  fun getLock(name: String): Lock
}
