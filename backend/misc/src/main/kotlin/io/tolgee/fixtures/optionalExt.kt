package io.tolgee.fixtures

import java.util.*

inline fun <reified T> Optional<T>.toNullable(): T? = this.orElse(null)
