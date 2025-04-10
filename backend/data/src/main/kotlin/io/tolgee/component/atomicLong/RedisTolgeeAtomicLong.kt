package io.tolgee.component.atomicLong

import io.tolgee.util.TolgeeAtomicLong
import org.redisson.api.RAtomicLong

class RedisTolgeeAtomicLong(
  private val it: RAtomicLong,
) : TolgeeAtomicLong {
  override fun addAndGet(delta: Long): Long = it.addAndGet(delta)

  override fun delete() {
    it.delete()
  }

  override fun get(): Long = it.get()
}
