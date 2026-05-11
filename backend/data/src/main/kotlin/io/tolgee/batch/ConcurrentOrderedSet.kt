package io.tolgee.batch

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe ordered set backed by a [LinkedHashSet].
 *
 * Maintains insertion order and rejects duplicates — [addLast] is a no-op if the element
 * is already present. This prevents the duplicate-jobId race that would occur when using
 * a plain [java.util.concurrent.ConcurrentLinkedDeque] as a round-robin order tracker.
 *
 * Lock ordering: callers inside [java.util.concurrent.ConcurrentHashMap.compute] may call
 * [addLast] or [remove] while holding the CHM key-lock. [pollFirst] acquires this lock
 * independently and never enters compute(). The acquisition order is therefore always
 * CHM-key-lock → this lock, never the reverse — no deadlock is possible.
 */
class ConcurrentOrderedSet<T> {
  private val lock = ReentrantLock()
  private val set = LinkedHashSet<T>()

  /** Appends [item] to the tail. Returns false (no-op) if already present. */
  fun addLast(item: T): Boolean = lock.withLock { set.add(item) }

  /** Removes and returns the head element, or null if empty. */
  fun pollFirst(): T? =
    lock.withLock {
      val iter = set.iterator()
      if (!iter.hasNext()) return null
      val item = iter.next()
      iter.remove()
      item
    }

  /** Returns the head element without removing it, or null if empty. */
  fun peekFirst(): T? = lock.withLock { set.firstOrNull() }

  /** Removes [item]. Returns false if it was not present. */
  fun remove(item: T): Boolean = lock.withLock { set.remove(item) }

  fun clear() = lock.withLock { set.clear() }
}
