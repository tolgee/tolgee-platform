package io.tolgee.testing

import io.tolgee.model.enums.Scope
import io.tolgee.testing.assertions.Assertions
import org.assertj.core.api.AbstractBigDecimalAssert
import org.assertj.core.api.AbstractBooleanAssert
import org.assertj.core.api.AbstractDateAssert
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.AbstractLongAssert
import org.assertj.core.api.AbstractStringAssert
import org.assertj.core.api.IterableAssert
import org.assertj.core.api.MapAssert
import org.assertj.core.api.ObjectArrayAssert
import org.assertj.core.api.ObjectAssert
import java.math.BigDecimal
import java.util.Date

inline val <reified T> T.assert: ObjectAssert<T>
  get() = Assertions.assertThat(this)

inline val <reified K, reified V> Map<K, V>.assert: MapAssert<K, V>
  get() = Assertions.assertThat(this)

inline val Int?.assert: AbstractIntegerAssert<*>
  get() = Assertions.assertThat(this)

inline val String?.assert: AbstractStringAssert<*>
  get() = Assertions.assertThat(this)

inline val Date?.assert: AbstractDateAssert<*> get() = Assertions.assertThat(this)
inline val <reified T> Iterable<T>?.assert: IterableAssert<T> get() = Assertions.assertThat(this)
inline val Long.assert: AbstractLongAssert<*> get() = Assertions.assertThat(this)

inline val Boolean?.assert: AbstractBooleanAssert<*>
  get() = Assertions.assertThat(this)

val Array<Scope>.assert: ObjectArrayAssert<Scope> get() = Assertions.assertThat(this)

inline val BigDecimal.assert: AbstractBigDecimalAssert<*>
  get() = Assertions.assertThat(this)
