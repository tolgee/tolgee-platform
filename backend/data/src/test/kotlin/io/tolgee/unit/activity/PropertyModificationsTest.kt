package io.tolgee.unit.activity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.data.PropertyModifications
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Verifies that [PropertyModifications] is byte-equivalent on the wire to
 * a plain `Map<String, PropertyModification>` — the
 * `activity_modified_entity.modifications` JSONB column is read back by
 * the activity feed UI and must round-trip cleanly.
 */
class PropertyModificationsTest {
  private val mapper = ObjectMapper().registerKotlinModule()

  // region Wire format — must match Map<String, PropertyModification>

  @Test
  fun `serializes empty as empty JSON object`() {
    val empty = PropertyModifications()
    assertThat(mapper.writeValueAsString(empty)).isEqualTo("{}")
  }

  @Test
  fun `serializes single primitive entry to legacy shape`() {
    val m = PropertyModifications().apply { put("name", "old", "new") }
    assertThat(mapper.writeValueAsString(m))
      .isEqualTo("""{"name":{"old":"old","new":"new"}}""")
  }

  @Test
  fun `serializes nulls in old and new`() {
    val m =
      PropertyModifications().apply {
        put("text", null, "hello")
        put("description", "was", null)
      }
    assertThat(mapper.writeValueAsString(m))
      .isEqualTo(
        """{"text":{"old":null,"new":"hello"},"description":{"old":"was","new":null}}""",
      )
  }

  @Test
  fun `serializes mixed value types`() {
    val m =
      PropertyModifications().apply {
        put("count", 1, 2)
        put("active", false, true)
        put("ratio", 0.5, 1.25)
        put("tagsAdded", listOf(1L, 2L), emptyList<Long>())
      }
    assertThat(mapper.writeValueAsString(m))
      .isEqualTo(
        """{"count":{"old":1,"new":2},"active":{"old":false,"new":true},""" +
          """"ratio":{"old":0.5,"new":1.25},"tagsAdded":{"old":[1,2],"new":[]}}""",
      )
  }

  @Test
  fun `preserves insertion order`() {
    val m =
      PropertyModifications().apply {
        put("z", 1, 2)
        put("a", 3, 4)
        put("m", 5, 6)
      }
    assertThat(mapper.writeValueAsString(m))
      .isEqualTo("""{"z":{"old":1,"new":2},"a":{"old":3,"new":4},"m":{"old":5,"new":6}}""")
  }

  @Test
  fun `re-putting the same key updates in place rather than appending`() {
    val m =
      PropertyModifications().apply {
        put("name", "v1", "v2")
        put("count", 1, 2)
        put("name", "v1-updated", "v2-updated")
      }
    assertThat(mapper.writeValueAsString(m))
      .isEqualTo(
        """{"name":{"old":"v1-updated","new":"v2-updated"},"count":{"old":1,"new":2}}""",
      )
  }

  // endregion

  // region Deserialization

  @Test
  fun `deserializes empty object`() {
    val deserialized = mapper.readValue("{}", PropertyModifications::class.java)
    assertThat(deserialized.isEmpty()).isTrue()
    assertThat(deserialized.size).isZero()
  }

  @Test
  fun `deserializes single entry`() {
    val deserialized =
      mapper.readValue(
        """{"name":{"old":"a","new":"b"}}""",
        PropertyModifications::class.java,
      )
    assertThat(deserialized.size).isEqualTo(1)
    assertThat(deserialized.containsKey("name")).isTrue()
    assertThat(deserialized.get("name")).isEqualTo(PropertyModification("a", "b"))
  }

  @Test
  fun `deserializes mixed types`() {
    val json =
      """{"count":{"old":1,"new":2},"active":{"old":false,"new":true},""" +
        """"text":{"old":null,"new":"hi"}}"""
    val d = mapper.readValue(json, PropertyModifications::class.java)
    assertThat(d.size).isEqualTo(3)
    assertThat(d.get("count")).isEqualTo(PropertyModification(1, 2))
    assertThat(d.get("active")).isEqualTo(PropertyModification(false, true))
    assertThat(d.get("text")).isEqualTo(PropertyModification(null, "hi"))
  }

  // endregion

  // region Round-trip equivalence with legacy Map<String, PropertyModification>

  @Test
  fun `round-trips through JSON identical to legacy Map shape`() {
    val legacy: Map<String, PropertyModification> =
      linkedMapOf(
        "name" to PropertyModification("old", "new"),
        "count" to PropertyModification(1, 2),
        "active" to PropertyModification(null, true),
      )
    val legacyJson = mapper.writeValueAsString(legacy)

    val compact =
      PropertyModifications().apply {
        put("name", "old", "new")
        put("count", 1, 2)
        put("active", null, true)
      }
    val compactJson = mapper.writeValueAsString(compact)

    assertThat(compactJson).isEqualTo(legacyJson)
  }

  @Test
  fun `parsing a compact-produced JSON via legacy Map type yields equivalent map`() {
    val compact =
      PropertyModifications().apply {
        put("text", "before", "after")
        put("priority", 1, 2)
      }
    val json = mapper.writeValueAsString(compact)

    val parsed: Map<String, PropertyModification> =
      mapper.readValue(
        json,
        mapper.typeFactory.constructMapType(
          LinkedHashMap::class.java,
          String::class.java,
          PropertyModification::class.java,
        ),
      )
    assertThat(parsed).hasSize(2)
    assertThat(parsed["text"]).isEqualTo(PropertyModification("before", "after"))
    assertThat(parsed["priority"]).isEqualTo(PropertyModification(1, 2))
  }

  // endregion

  // region Map-like API used by the activity interceptor

  @Test
  fun `containsKey reflects current state`() {
    val m = PropertyModifications().apply { put("a", 1, 2) }
    assertThat(m.containsKey("a")).isTrue()
    assertThat(m.containsKey("b")).isFalse()
  }

  @Test
  fun `keys returns all current names`() {
    val m =
      PropertyModifications().apply {
        put("a", 1, 2)
        put("b", 3, 4)
      }
    assertThat(m.keys).containsExactlyInAnyOrder("a", "b")
  }

  @Test
  fun `addAll from a legacy Map merges entries`() {
    val m = PropertyModifications().apply { put("a", 1, 2) }
    m.addAll(
      linkedMapOf(
        "b" to PropertyModification(3, 4),
        "a" to PropertyModification(10, 20),
      ),
    )
    assertThat(m.size).isEqualTo(2)
    assertThat(m["a"]).isEqualTo(PropertyModification(10, 20))
    assertThat(m["b"]).isEqualTo(PropertyModification(3, 4))
  }

  @Test
  fun `is a Map view so AssertJ MapAssert and entries access work`() {
    val m =
      PropertyModifications().apply {
        put("name", "old", "new")
        put("count", 1, 2)
      }
    val asMap: Map<String, PropertyModification> = m
    assertThat(asMap).hasSize(2)
    assertThat(asMap.values).extracting<Any?> { it.new }.containsExactly("new", 2)
    assertThat(asMap.entries.map { it.key }).containsExactly("name", "count")
  }

  // endregion

  // region Shared name registry — companion-level map keyed by entity class

  @Test
  fun `instances of the same entity class share the underlying name list`() {
    val a = PropertyModifications("Translation")
    val b = PropertyModifications("Translation")
    a.put("text", "x", "y")
    b.put("state", "REVIEWED", "TRANSLATED")
    // Each instance keeps its own per-instance entries (the modifications it actually saw),
    // but the underlying name strings come from the shared registry — so the same String
    // instance is reused across instances of the same entity class.
    assertThat(a.keys.first()).isSameAs(b.lookupSharedName("text"))
    assertThat(b.keys.first()).isSameAs(a.lookupSharedName("state"))
  }

  @Test
  fun `instances of different entity classes do not share the registry`() {
    val translation = PropertyModifications("Translation")
    val key = PropertyModifications("Key")
    translation.put("text", "a", "b")
    // Adding "text" to Key creates a separate entry under Key's registry.
    assertThat(key.lookupSharedName("text")).isNull()
    key.put("text", "x", "y")
    // After adding to Key, both registries have "text" but they are independent ArrayList instances.
    assertThat(translation.lookupSharedName("text")).isNotNull
    assertThat(key.lookupSharedName("text")).isNotNull
  }

  @Test
  fun `serialization is independent of class-keyed sharing`() {
    val m =
      PropertyModifications("Translation").apply {
        put("text", "old", "new")
        put("state", null, "REVIEWED")
      }
    assertThat(mapper.writeValueAsString(m))
      .isEqualTo("""{"text":{"old":"old","new":"new"},"state":{"old":null,"new":"REVIEWED"}}""")
  }

  @Test
  fun `each instance iterates only its own entries in insertion order`() {
    val a = PropertyModifications("Translation").apply { put("text", 1, 2) }
    val b = PropertyModifications("Translation").apply { put("state", 1, 2) }
    // Even though the shared registry now contains both "text" and "state",
    // each instance only exposes the keys it explicitly put.
    assertThat(a.keys).containsExactly("text")
    assertThat(b.keys).containsExactly("state")
  }

  // endregion
}
