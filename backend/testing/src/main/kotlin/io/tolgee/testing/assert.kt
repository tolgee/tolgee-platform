package io.tolgee.testing

import io.tolgee.testing.assertions.Assertions
import org.assertj.core.api.AbstractDateAssert
import org.assertj.core.api.ObjectAssert
import java.util.*

inline val <reified T> T.assert: ObjectAssert<T>
  get() = Assertions.assertThat(this)

inline val Date?.assert: AbstractDateAssert<*> get() = Assertions.assertThat(this)
