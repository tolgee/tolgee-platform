package io.tolgee.testing

import io.tolgee.testing.assertions.Assertions
import org.assertj.core.api.AbstractBooleanAssert
import org.assertj.core.api.AbstractDateAssert
import org.assertj.core.api.AbstractLongAssert
import org.assertj.core.api.IterableAssert
import org.assertj.core.api.ObjectAssert
import java.util.*

inline val <reified T> T.assert: ObjectAssert<T>
  get() = Assertions.assertThat(this)

inline val Date?.assert: AbstractDateAssert<*> get() = Assertions.assertThat(this)
inline val <reified T> Iterable<T>.assert: IterableAssert<T> get() = Assertions.assertThat(this)
inline val Long.assert: AbstractLongAssert<*> get() = Assertions.assertThat(this)

inline val Boolean?.assert: AbstractBooleanAssert<*>
  get() = Assertions.assertThat(this)
