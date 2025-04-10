package io.tolgee.util

fun <T : Iterable<*>> T.nullIfEmpty(): T? = if (this.none()) null else this
