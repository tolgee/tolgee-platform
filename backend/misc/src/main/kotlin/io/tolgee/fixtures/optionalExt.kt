package io.tolgee.fixtures

import java.util.Optional

inline fun <reified T> Optional<T>.toNullable(): T? {
  return this.orElse(null)
}
