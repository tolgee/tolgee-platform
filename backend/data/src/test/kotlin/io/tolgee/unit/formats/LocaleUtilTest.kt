package io.tolgee.unit.formats

import io.tolgee.formats.getULocaleFromTag
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class LocaleUtilTest {
  @Test
  fun `return correct ULocale from tag`() {
    getULocaleFromTag("cs").toLanguageTag().assert.isEqualTo("cs")
    getULocaleFromTag("ar-bla").toLanguageTag().assert.isEqualTo("ar")
    getULocaleFromTag("en-US").toLanguageTag().assert.isEqualTo("en-US")
    getULocaleFromTag("hy-Latn-IT-arevela").toLanguageTag().assert.isEqualTo("hy-Latn-IT-arevela")
  }
}
