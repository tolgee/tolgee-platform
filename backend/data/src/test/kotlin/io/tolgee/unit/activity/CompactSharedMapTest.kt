package io.tolgee.unit.activity

import io.tolgee.activity.data.DescribingDataMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Drives every public/internal method of [io.tolgee.activity.data.CompactSharedMap]
 * through its [DescribingDataMap] subclass (anonymous schema — isolated per instance).
 */
class CompactSharedMapTest {
  // region size / isEmpty / isNotEmpty

  @Test
  fun `empty map reports zero size and isEmpty`() {
    val m = DescribingDataMap()
    assertThat(m.size).isZero()
    assertThat(m.isEmpty()).isTrue()
    assertThat(m.isNotEmpty()).isFalse()
  }

  @Test
  fun `after put isEmpty is false and isNotEmpty is true`() {
    val m = DescribingDataMap().apply { put("a", 1) }
    assertThat(m.size).isEqualTo(1)
    assertThat(m.isEmpty()).isFalse()
    assertThat(m.isNotEmpty()).isTrue()
  }

  @Test
  fun `size matches number of distinct keys`() {
    val m =
      DescribingDataMap().apply {
        put("a", 1)
        put("b", 2)
        put("c", 3)
      }
    assertThat(m.size).isEqualTo(3)
  }

  // endregion

  // region containsKey

  @Test
  fun `containsKey true for present key`() {
    val m = DescribingDataMap().apply { put("a", 1) }
    assertThat(m.containsKey("a")).isTrue()
  }

  @Test
  fun `containsKey false for unknown key`() {
    val m = DescribingDataMap().apply { put("a", 1) }
    assertThat(m.containsKey("b")).isFalse()
  }

  @Test
  fun `containsKey false on empty map`() {
    assertThat(DescribingDataMap().containsKey("a")).isFalse()
  }

  // endregion

  // region containsValue

  @Test
  fun `containsValue true for present primitive value`() {
    val m = DescribingDataMap().apply { put("a", 42) }
    assertThat(m.containsValue(42)).isTrue()
  }

  @Test
  fun `containsValue true for present null value`() {
    val m = DescribingDataMap().apply { put("a", null) }
    assertThat(m.containsValue(null)).isTrue()
  }

  @Test
  fun `containsValue false when value not present`() {
    val m = DescribingDataMap().apply { put("a", 1) }
    assertThat(m.containsValue(2)).isFalse()
    assertThat(m.containsValue("1")).isFalse()
  }

  // endregion

  // region get

  @Test
  fun `get returns stored value`() {
    val m = DescribingDataMap().apply { put("name", "x") }
    assertThat(m["name"]).isEqualTo("x")
  }

  @Test
  fun `get returns null for unknown key`() {
    assertThat(DescribingDataMap()["missing"]).isNull()
  }

  @Test
  fun `get distinguishes null value from missing key`() {
    val m = DescribingDataMap().apply { put("a", null) }
    assertThat(m["a"]).isNull()
    assertThat(m.containsKey("a")).isTrue()
    assertThat(m["b"]).isNull()
    assertThat(m.containsKey("b")).isFalse()
  }

  // endregion

  // region put

  @Test
  fun `put adds new entry`() {
    val m = DescribingDataMap()
    m.put("a", 1)
    assertThat(m["a"]).isEqualTo(1)
    assertThat(m.size).isEqualTo(1)
  }

  @Test
  fun `put updates existing key in place without growing size`() {
    val m =
      DescribingDataMap().apply {
        put("a", 1)
        put("b", 2)
      }
    m.put("a", 99)
    assertThat(m.size).isEqualTo(2)
    assertThat(m["a"]).isEqualTo(99)
    assertThat(m["b"]).isEqualTo(2)
    assertThat(m.keys).containsExactly("a", "b")
  }

  @Test
  fun `put preserves insertion order across keys`() {
    val m =
      DescribingDataMap().apply {
        put("z", 1)
        put("a", 2)
        put("m", 3)
      }
    assertThat(m.keys).containsExactly("z", "a", "m")
  }

  // endregion

  // region addAll

  @Test
  fun `addAll copies entries from another map`() {
    val m = DescribingDataMap().apply { put("a", 1) }
    m.addAll(linkedMapOf("b" to 2, "c" to 3))
    assertThat(m.size).isEqualTo(3)
    assertThat(m["a"]).isEqualTo(1)
    assertThat(m["b"]).isEqualTo(2)
    assertThat(m["c"]).isEqualTo(3)
  }

  @Test
  fun `addAll overwrites duplicate keys`() {
    val m = DescribingDataMap().apply { put("a", 1) }
    m.addAll(mapOf("a" to 99))
    assertThat(m.size).isEqualTo(1)
    assertThat(m["a"]).isEqualTo(99)
  }

  // endregion

  // region keys / values / entries

  @Test
  fun `keys reflects insertion order and is a snapshot`() {
    val m =
      DescribingDataMap().apply {
        put("a", 1)
        put("b", 2)
      }
    val snapshot = m.keys
    m.put("c", 3)
    assertThat(snapshot).containsExactly("a", "b")
    assertThat(m.keys).containsExactly("a", "b", "c")
  }

  @Test
  fun `values reflects insertion order`() {
    val m =
      DescribingDataMap().apply {
        put("a", "x")
        put("b", "y")
      }
    assertThat(m.values).containsExactly("x", "y")
  }

  @Test
  fun `entries reflect insertion order and pair keys with values`() {
    val m =
      DescribingDataMap().apply {
        put("a", 1)
        put("b", 2)
      }
    assertThat(m.entries.map { it.key to it.value })
      .containsExactly("a" to 1, "b" to 2)
  }

  // endregion

  // region Map interface conformance

  @Test
  fun `usable through the Map interface`() {
    val m: Map<String, Any?> =
      DescribingDataMap().apply {
        put("a", 1)
        put("b", 2)
      }
    assertThat(m).hasSize(2).containsEntry("a", 1).containsEntry("b", 2)
  }

  // endregion

  // region Capacity growth

  @Test
  fun `grows beyond initial valuesArray capacity without losing entries`() {
    val m = DescribingDataMap()
    val n = 200
    for (i in 0 until n) m.put("k$i", i)
    assertThat(m.size).isEqualTo(n)
    for (i in 0 until n) assertThat(m["k$i"]).isEqualTo(i)
    assertThat(m.keys.first()).isEqualTo("k0")
    assertThat(m.keys.last()).isEqualTo("k${n - 1}")
  }

  // endregion

  // region lookupSharedName

  @Test
  fun `lookupSharedName returns the interned String for a put key`() {
    val m = DescribingDataMap().apply { put("name", 1) }
    val shared = m.lookupSharedName("name")
    assertThat(shared).isNotNull
    // The keys snapshot is built from the schema, so its String instance
    // is the same instance the registry holds.
    assertThat(m.keys.first()).isSameAs(shared)
  }

  @Test
  fun `lookupSharedName returns null for an unknown key`() {
    val m = DescribingDataMap().apply { put("name", 1) }
    assertThat(m.lookupSharedName("other")).isNull()
  }

  // endregion
}
