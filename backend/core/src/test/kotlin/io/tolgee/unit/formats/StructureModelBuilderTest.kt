package io.tolgee.unit.formats

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.fixtures.node
import io.tolgee.formats.nestedStructureModel.StructureModelBuilder
import io.tolgee.testing.assert
import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StructureModelBuilderTest {
  @Test
  fun `test simple flat string`() {
    listOf("a").testResult('.', true) {
      node("a").isEqualTo("text")
    }
  }

  @Test
  fun `test simple nested string`() {
    listOf("a.a").testResult('.', true) { node("a.a").isEqualTo("text") }
  }

  @Test
  fun `test simple nested array`() {
    listOf("a[10]").testResult('.', true) { node("a[0]").isEqualTo("text") }
  }

  @Test
  fun `test root array`() {
    listOf("[10]").testResult('.', true) { node("[0]").isEqualTo("text") }
  }

  @Test
  fun `test root array and nested`() {
    listOf("[10].a").testResult('.', true) { node("[0].a").isEqualTo("text") }
  }

  @Test
  fun `test multiple nesting levels arrays outer`() {
    listOf("[10][10].h.e.l.l.o[10]").testResult('.', true) {
      node("[0].[0].h.e.l.l.o.[0]").isEqualTo("text")
    }
  }

  @Test
  fun `test multiple nesting objects outer`() {
    listOf("h.e[10][10].l.o").testResult('.', true) {
      node("h.e.[0].[0].l.o").isEqualTo("text")
    }
  }

  @Test
  fun `it handles collision - string with object`() {
    listOf("h", "h.h").testResult('.', true) {
      node("h").isEqualTo("text")
      node("h\\.h").isEqualTo("text")
    }
  }

  @Test
  fun `it handles collision - string with object (nested)`() {
    listOf("h.h", "h.h.h").testResult('.', true) {
      node("h.h").isEqualTo("text")
      node("h.h\\.h").isEqualTo("text")
    }
  }

  @Test
  fun `it handles collision - string with array`() {
    listOf("h", "h[10]").testResult('.', true) {
      node("h").isEqualTo("text")
      isObject.containsKeys("h[10]")
    }
  }

  @Test
  fun `it handles collision - string with array (nested)`() {
    listOf("h.h", "h.h[10]").testResult('.', true) {
      node("h") {
        node("h").isEqualTo("text")
        isObject.containsKeys("h[10]")
      }
    }
  }

  @Test
  fun `it handles collision - string with object inside array`() {
    listOf("[10]", "[10].h").testResult('.', true) {
      node("[0]").isEqualTo("text")
      node("[1].h").isEqualTo("text")
    }
  }

  @Test
  fun `it handles collision - root object with array`() {
    listOf("a", "[10]").testResult('.', true) {
      node("a").isEqualTo("text")
      isObject.containsKeys("[10]")
    }
  }

  @Test
  fun `it handles collision - object with array first nesting level`() {
    listOf("a.a", "a[10]").testResult('.', true) {
      node("a") {
        isObject.containsKeys("a", "[10]")
      }
    }
  }

  @Test
  fun `it handles collision - object with array second nesting level`() {
    listOf("a.a.a", "a.a[10]").testResult('.', true) {
      node("a.a") {
        node("a").isEqualTo("text")
        isObject.containsKeys("[10]")
      }
    }
  }

  @Test
  fun `it handles collision - root array with object`() {
    listOf("[10]", "a").testResult('.', true) {
      isObject.containsKeys("[10]")
      node("a").isEqualTo("text")
    }
  }

  @Test
  fun `it handles collision - array with object in first nesting level`() {
    listOf("a[10]", "a.a").testResult('.', true) {
      node("a").isObject.containsKeys("[10]", "a")
    }
  }

  @Test
  fun `it handles collision - array with object in first nesting level (in another array)`() {
    listOf("[10][10]", "[10].a").testResult('.', true) {
      node("[0]").isObject.containsKeys("[10]", "a")
    }
  }

  @Test
  fun `it handles collision - array with object in second nesting level`() {
    listOf("a.a[10]", "a.a.a").testResult('.', true) {
      node("a.a") {
        isObject.containsKeys("[10]", "a")
      }
    }
  }

  @Test
  fun `it handles collision - throws when not sorted - object with text`() {
    assertThrows<IllegalStateException> {
      listOf("a.a", "a").testResult('.', true)
    }.message.assert.contains("Path: a")
  }

  @Test
  fun `it handles collision - throws when not sorted - array with text`() {
    assertThrows<IllegalStateException> {
      listOf("a[10]", "a").testResult('.', true)
    }.message.assert.contains("Path: a")
  }

  @Test
  fun `adds multiple items to root array`() {
    listOf("[10]", "[11]").testResult('.', true) {
      isArray.hasSize(2)
    }
  }

  @Test
  fun `adds multiple items to nested array`() {
    listOf("a[10]", "a[11]").testResult('.', true) {
      node("a").isArray.hasSize(2)
    }
  }

  @Test
  fun `it works with complex case`() {
    listOf(
      "[10]",
      "a",
      "a.a",
      "a.a.a",
      "a.a.b",
      "[11]",
      "a[10]",
      "a[11]",
      "b.a",
      "b.c",
      "b[3]",
      "d[10].a.b[10].a.c",
      "e[1]",
      "e[2]",
    ).sortedBy { it }.testResult('.', true) {
      isObject.containsKeys("[10]", "[11]", "a", "a.a", "a.a.a", "a.a.b", "a[10]", "a[11]")
      node("b") {
        isObject.containsKeys("a", "c", "[3]")
      }
      node("d[0].a.b[0].a.c").isEqualTo("text")
      node("e") {
        isArray.hasSize(2)
      }
    }
  }

  @Test
  fun `handles collision - root and nested array`() {
    listOf("[10].a.b", "[10].a[10]").testResult('.', true) {
      node("[0].a").isObject.containsKeys("b", "[10]")
    }
  }

  @Test
  fun `rootKeyIsLanguageTag works`() {
    listOf("h", "h[10]").testResult('.', true, rootKeyIsLanguageTag = true) {
      node("en") {
        node("h").isEqualTo("text")
        isObject.containsKeys("h[10]")
      }
    }
  }

  @Test
  fun `nested plurals work`() {
    listOf(
      "h",
      PluralValue("a", mapOf("one" to "I have one dog", "other" to "I have %d dogs")),
    ).testResult(
      '.',
      true,
      rootKeyIsLanguageTag = true,
    ) {
      node("en") {
        node("h").isEqualTo("text")
        node("a") {
          node("one").isEqualTo("I have one dog")
          node("other").isEqualTo("I have %d dogs")
        }
      }
    }

    listOf(
      "h",
      PluralValue("a", mapOf("one" to "I have one dog", "other" to "I have %d dogs")),
    ).testResult(
      '.',
      true,
      rootKeyIsLanguageTag = false,
    ) {
      node("h").isEqualTo("text")
      node("a") {
        node("one").isEqualTo("I have one dog")
        node("other").isEqualTo("I have %d dogs")
      }
    }
  }

  @Test
  fun `does not throw on collision between plural and non-plural`() {
    listOf(
      PluralValue("key", mapOf("other" to "Collision!")),
      "key.other",
    ).testResult(
      '.',
      true,
      rootKeyIsLanguageTag = true,
    ) {
      node("en") {
        isObject.hasSize(1)
        node("key.other").isEqualTo("Collision!")
      }
    }
  }

  class PluralValue(
    val key: String,
    val pluralForms: Map<String, String>,
  )

  private inline fun List<Any>.testResult(
    delimiter: Char,
    supportArrays: Boolean,
    rootKeyIsLanguageTag: Boolean = false,
    assertFn: JsonAssert.ConfigurableJsonAssert.() -> Unit = {},
  ): JsonAssert.ConfigurableJsonAssert {
    val builder = StructureModelBuilder(delimiter, supportArrays, rootKeyIsLanguageTag)
    this.forEach {
      (it as? String)?.let { string ->
        builder.addValue("en", string, "text")
      }
      (it as? PluralValue)?.let { pluralValue ->
        builder.addValue("en", pluralValue.key, pluralValue.pluralForms)
      }
    }
    val result = builder.result
    val jsonString = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result)
    val assert =
      assertThatJson(
        jsonString,
      )
    try {
      assertFn(assert)
    } catch (e: AssertionError) {
      println(jsonString)
      throw e
    }
    return assert
  }
}
