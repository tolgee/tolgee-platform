package io.tolgee.ee.unit

import io.tolgee.ee.component.PromptLazyMap
import io.tolgee.ee.component.PromptLazyMap.Companion.Variable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PromptLazyMapTest {
  private fun buildMap(vararg pairs: Pair<String, Variable>): PromptLazyMap {
    val map = PromptLazyMap()
    map.setMap(pairs.toMap())
    return map
  }

  @Test
  fun `returns value when no lazyValue`() {
    val map = buildMap("key" to Variable(name = "key", value = "hello"))
    assertThat(map["key"].toString()).isEqualTo("hello")
  }

  @Test
  fun `evaluates lazyValue when value is null`() {
    val map = buildMap("key" to Variable(name = "key", lazyValue = { "computed" }))
    assertThat(map["key"].toString()).isEqualTo("computed")
  }

  @Test
  fun `lazyValue is evaluated only once`() {
    var callCount = 0
    val map =
      buildMap(
        "key" to
          Variable(
            name = "key",
            lazyValue = {
              callCount++
              "result"
            },
          ),
      )

    map["key"]
    map["key"]
    map["key"]

    assertThat(callCount).isEqualTo(1)
  }
}
