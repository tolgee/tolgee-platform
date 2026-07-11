package io.tolgee.fixtures

import org.assertj.core.api.AbstractAssert

fun <T : AbstractAssert<T, A>, A> AbstractAssert<T, A>.satisfies(fn: (e: A) -> Any?) {
  satisfies({ fn(it) })
}
