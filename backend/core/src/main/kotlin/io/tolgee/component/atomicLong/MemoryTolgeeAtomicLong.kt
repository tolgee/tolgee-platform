package io.tolgee.component.atomicLong

import io.tolgee.util.TolgeeAtomicLong
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class MemoryTolgeeAtomicLong(
  private val name: String,
  private val defaultProvider: () -> Long,
) : TolgeeAtomicLong {
  companion object {
    private val map = ConcurrentHashMap<String, AtomicLong>()
  }

  private val it: AtomicLong by lazy { map.getOrPut(name) { AtomicLong(defaultProvider()) } }

  override fun addAndGet(delta: Long): Long {
    return it.addAndGet(delta)
  }

  override fun delete() {
    map.remove(name)
  }

  override fun get(): Long {
    return it.get()
  }
}
