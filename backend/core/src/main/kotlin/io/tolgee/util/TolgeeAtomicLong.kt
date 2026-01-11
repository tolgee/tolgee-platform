package io.tolgee.util

interface TolgeeAtomicLong {
  fun addAndGet(delta: Long): Long

  fun delete()

  fun get(): Long
}
