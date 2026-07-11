package io.tolgee.util

fun <T : Iterable<*>> T.nullIfEmpty(): T? {
  return if (this.none()) null else this
}

fun <T : CharSequence> T.nullIfEmpty(): T? {
  return if (this.none()) null else this
}
