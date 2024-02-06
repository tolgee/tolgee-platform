package io.tolgee.unit.formats

import io.tolgee.formats.getPluralFormsForLocale
import io.tolgee.formats.isSamePossiblePlural
import io.tolgee.formats.optimizePluralForms
import io.tolgee.formats.optimizePossiblePlural
import io.tolgee.formats.orderPluralForms
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PluralsFormUtilTest {
  @Test
  fun `return correct plural forms for locale`() {
    getPluralFormsForLocale("cs").assert.containsExactly("one", "few", "many", "other")
    getPluralFormsForLocale("en").assert.containsExactly("one", "other")
    getPluralFormsForLocale("ar").assert.containsExactly("zero", "one", "two", "few", "many", "other")
    // non-existing weird tag, should fall back to "ar"
    getPluralFormsForLocale("ar-BLA").assert.containsExactly("zero", "one", "two", "few", "many", "other")
  }

  @Test
  fun `orders plural forms`() {
    orderPluralForms(mapOf("many" to "", "one" to "", "=1" to "", "zero" to ""))
      .keys.assert.containsExactly("zero", "one", "many", "=1")
  }

  @Test
  fun `optimizes the forms`() {
    optimizePluralForms(mapOf("other" to "same", "many" to "same", "one" to "different", "zero" to "same"))
      .keys.assert.containsExactly("one", "other")
  }

  @Test
  fun `optimizes the ICU plural`() {
    optimizePossiblePlural("{0, plural, one {same} other {other} many {same} few {same}}")
      .assert.isEqualTo("{0, plural,\none {same}\nfew {same}\nmany {same}\nother {other}\n}")

    optimizePossiblePlural("{0, plural, one {same} other {same} many {same} few {same}}")
      .assert.isEqualTo("{0, plural,\nother {same}\n}")
  }

  @Test
  fun `compares plurals correctly`() {
    (
      "I have {count, plural, other {# dogs} one {# dog} many {# dogs}}." isSamePossiblePlural
        "I have {count, plural, other {# dogs} one {# dog}}."
    ).assert.isTrue()

    (
      "I have {count, plural, other {# dogs} one {# dog} many {# dogos}}." isSamePossiblePlural
        "I have {count, plural, other {# dogs} one {# dog}}."
    ).assert.isFalse()

    ("I have dogs." isSamePossiblePlural "I have dogs.").assert.isTrue()
    (null isSamePossiblePlural "I have dogs").assert.isFalse()
    ("I have dogs" isSamePossiblePlural null).assert.isFalse()
    (null isSamePossiblePlural null).assert.isTrue()
  }
}
