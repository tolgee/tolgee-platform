package io.tolgee.unit.formats

import io.tolgee.formats.capitalizesAllNouns
import io.tolgee.formats.isWrittenInCaselessScript
import io.tolgee.formats.isWrittenInNonLatinScript
import io.tolgee.formats.javaLocaleOf
import io.tolgee.formats.languageCodeOf
import io.tolgee.formats.pluralVariantMatchesSingleInteger
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class LanguageTraitsUtilTest {
  @Test
  fun `detects languages written in a script without letter case`() {
    listOf("ja", "ko", "zh", "zh-Hans", "zh-TW", "ar", "he", "fa", "th", "hi", "ka", "pa-Arab").forEach {
      it.assert.matches(::isWrittenInCaselessScript, "written in a caseless script")
    }
    listOf("en", "de-DE", "cs", "ru", "uk", "sr", "sr-Latn", "el", "hy", "tr", "vi").forEach {
      it.assert.matches({ tag -> !isWrittenInCaselessScript(tag) }, "written in a cased script")
    }
  }

  @Test
  fun `detects languages written in a non-Latin script`() {
    listOf("ru", "el", "sr", "ja", "zh-Hans", "ko", "ar", "he", "hi").forEach {
      it.assert.matches(::isWrittenInNonLatinScript, "written in a non-Latin script")
    }
    listOf("en", "de-DE", "cs", "tr", "vi", "sr-Latn").forEach {
      it.assert.matches({ tag -> !isWrittenInNonLatinScript(tag) }, "written in the Latin script")
    }
  }

  @Test
  fun `treats a language tag without a known script as cased and Latin`() {
    listOf("cz", "ua", "jp", "cn", "xx-custom", "klingon").forEach {
      it.assert.matches({ tag -> !isWrittenInCaselessScript(tag) }, "not treated as caseless")
      it.assert.matches({ tag -> !isWrittenInNonLatinScript(tag) }, "not treated as non-Latin")
    }
  }

  @Test
  fun `detects languages that capitalize all nouns`() {
    listOf("de", "de-DE", "de-AT", "lb").forEach {
      it.assert.matches(::capitalizesAllNouns, "capitalizes all nouns")
    }
    listOf("en", "nl", "da", "sv").forEach {
      it.assert.matches({ tag -> !capitalizesAllNouns(tag) }, "does not capitalize all nouns")
    }
  }

  @Test
  fun `detects plural variants used for a single integer`() {
    listOf(
      "ar" to "zero",
      "ar" to "one",
      "ar" to "two",
      "he" to "one",
      "he" to "two",
      "en" to "one",
      "cs" to "one",
      "cy" to "few",
      "cy" to "many",
      "fr" to "=0",
      "ru" to "=5",
    ).forEach { variant ->
      variant.assert.matches({ (tag, key) -> pluralVariantMatchesSingleInteger(tag, key) }, "matches a single integer")
    }
  }

  @Test
  fun `detects plural variants used for several integers`() {
    listOf(
      "ar" to "few",
      "ar" to "many",
      "ar" to "other",
      "he" to "other",
      "fr" to "one",
      "fr" to "many",
      "pt" to "one",
      "ru" to "one",
      "sl" to "one",
      "lv" to "zero",
      "br" to "few",
      "ja" to "other",
      "en" to "few",
    ).forEach { variant ->
      variant.assert.matches({ (tag, key) -> !pluralVariantMatchesSingleInteger(tag, key) }, "matches several integers")
    }
  }

  @Test
  fun `understands language tags written with an underscore`() {
    "zh_CN".assert.matches(::isWrittenInCaselessScript, "written in a caseless script")
    "ar_SA".assert.matches(::isWrittenInNonLatinScript, "written in a non-Latin script")
    "de_DE".assert.matches(::capitalizesAllNouns, "capitalizes all nouns")
    languageCodeOf("el_GR").assert.isEqualTo("el")
    languageCodeOf("pt-BR").assert.isEqualTo("pt")
    javaLocaleOf("tr_TR").toLanguageTag().assert.isEqualTo("tr-TR")
    ("ar_SA" to "two").assert.matches({ (tag, key) -> pluralVariantMatchesSingleInteger(tag, key) }, "single integer")
  }
}
