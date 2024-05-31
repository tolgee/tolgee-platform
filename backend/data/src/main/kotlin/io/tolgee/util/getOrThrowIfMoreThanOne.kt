package io.tolgee.util

/**
 * Throws exception if there is more than one element in the collection
 */
inline fun <reified T : Any> Iterable<T>.getOrThrowIfMoreThanOne(exceptionProvider: () -> Exception): T? {
  if (this.count() > 1) {
    throw exceptionProvider()
  }

  return this.firstOrNull()
}
