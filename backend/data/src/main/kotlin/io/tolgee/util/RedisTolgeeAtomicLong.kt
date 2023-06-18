package io.tolgee.util

import org.redisson.api.RAtomicLong

class RedisTolgeeAtomicLong(
  private val it: RAtomicLong
) : TolgeeAtomicLong {
  override fun addAndGet(delta: Long): Long {
    return it.addAndGet(delta)
  }

  override fun delete() {
    it.delete()
  }

  override fun get(): Long {
    return it.get()
  }
}
