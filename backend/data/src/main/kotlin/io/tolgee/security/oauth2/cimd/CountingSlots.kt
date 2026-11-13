/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.oauth2.cimd

import java.util.concurrent.ConcurrentHashMap

/** How many slots one key holds at a time, capped at [max]. */
internal class CountingSlots(
  private val max: Int,
) {
  private val counts = ConcurrentHashMap<String, Int>()

  fun take(key: String): Boolean {
    var taken = false
    counts.compute(key) { _, count ->
      val current = count ?: 0
      // Returning null here would drop the entry and reset the count while the slots are still held.
      if (current >= max) return@compute current
      taken = true
      current + 1
    }
    return taken
  }

  /** Removes the entry at zero so the map cannot be grown by naming keys. */
  fun release(key: String) {
    counts.computeIfPresent(key) { _, count -> (count - 1).takeIf { it > 0 } }
  }

  internal fun trackedKeys(): Int = counts.size
}
