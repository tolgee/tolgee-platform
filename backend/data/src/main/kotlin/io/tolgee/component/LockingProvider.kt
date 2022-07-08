package io.tolgee.component

import java.util.concurrent.locks.Lock

interface LockingProvider {
  fun getLock(name: String): Lock
}
