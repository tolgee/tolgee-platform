package io.tolgee.unit.formats

import io.tolgee.formats.getPluralFormExamples
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PluralFormExamplesUtilTest {
  @Test
  fun `returns correct examples`() {
    getPluralFormExamples("cs").assert.isEqualTo(
      mapOf("one" to 1, "few" to 2, "many" to 0.5, "other" to 10),
    )
    getPluralFormExamples("en").assert.isEqualTo(
      mapOf("one" to 1, "other" to 10),
    )
    getPluralFormExamples("ar-weird-tag").assert.isEqualTo(
      mapOf("few" to 3, "many" to 11, "one" to 1, "other" to 100, "two" to 2, "zero" to 0),
    )
    getPluralFormExamples("es").assert.isEqualTo(
      mapOf("many" to 1000000, "one" to 1, "other" to 10),
    )
  }
}
