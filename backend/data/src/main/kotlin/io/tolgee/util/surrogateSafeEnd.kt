package io.tolgee.util

/**
 * The index to cut [value] at so the result never ends in a lone high surrogate - pgjdbc rejects
 * one, where `@Size` would accept it.
 */
fun surrogateSafeEnd(
  value: String,
  end: Int,
): Int {
  val bounded = end.coerceIn(0, value.length)
  if (bounded > 0 && value[bounded - 1].isHighSurrogate()) {
    return bounded - 1
  }
  return bounded
}
