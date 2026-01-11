package io.tolgee.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.NoOpFromIcuPlaceholderConvertor
import io.tolgee.formats.optimizePossiblePlural
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.testing.assert

fun FileProcessorContext.assertTranslations(
  language: String,
  key: String,
): List<ImportTranslationInContextAssertions> {
  val translations = this.translations[key] ?: throw AssertionError("Translation with key $key not found")
  translations.filter { it.language.name == language }.let { translationsOfKey ->
    if (translationsOfKey.isEmpty()) {
      throw AssertionError("Translation with key $key and language $language not found")
    }
    return translationsOfKey.map { ImportTranslationInContextAssertions(this, it, key) }
  }
}

fun List<ImportTranslationInContextAssertions>.assertSize(size: Int): List<ImportTranslationInContextAssertions> {
  this.size.assert.isEqualTo(size)
  return this
}

fun List<ImportTranslationInContextAssertions>.assertSinglePlural(
  fn: ImportTranslationInContextAssertions.() -> Unit,
): ImportTranslationInContextAssertions {
  val filtered = this.filter { it.getPossiblePlural().isPlural() }
  filtered.assertSize(1)
  filtered[0].isPlural(true)
  fn(filtered[0])
  return filtered[0]
}

fun List<ImportTranslationInContextAssertions>.assertSingle(
  fn: ImportTranslationInContextAssertions.() -> Unit,
): ImportTranslationInContextAssertions {
  this.assertSize(1)
  fn(this[0])
  return this[0]
}

fun List<ImportTranslationInContextAssertions>.assertAllSame(
  fn: ImportTranslationInContextAssertions.() -> Unit,
): ImportTranslationInContextAssertions {
  if (this.map { it.translation.text }.toSet().size == 1) {
    fn(this[0])
    return this[0]
  }
  throw AssertionError("Not all translations are the same")
}

fun List<ImportTranslationInContextAssertions>.assertMultiple(
  fn: List<ImportTranslationInContextAssertions>.() -> Unit = {},
): List<ImportTranslationInContextAssertions> {
  this.assert.hasSizeGreaterThan(1)
  fn(this)
  return this
}

fun FileProcessorContext.assertLanguagesCount(languagesCount: Int): FileProcessorContext {
  this.languages.size.assert
    .isEqualTo(languagesCount)
  return this
}

fun ImportTranslationInContextAssertions.hasKeyDescription(description: String) {
  val keyMeta =
    this.fileProcessorContext.keys[keyName]?.keyMeta
      ?: throw AssertionError("Key meta not found")
  keyMeta.description.assert.isEqualTo(description)
}

fun ImportTranslationInContextAssertions.isPlural(isPlural: Boolean = true) {
  this.translation.isPlural.assert
    .isEqualTo(isPlural)
}

data class ImportTranslationInContextAssertions(
  val fileProcessorContext: FileProcessorContext,
  val translation: ImportTranslation,
  val keyName: String,
) {
  fun hasText(text: String): ImportTranslationInContextAssertions {
    this.translation.text.assert
      .isEqualTo(text)
    return this
  }

  private fun assertTextNotNull(): ImportTranslationInContextAssertions {
    this.translation.text ?: throw AssertionError("Text is null")
    return this
  }

  fun getPossiblePlural() =
    this.translation.text!!.let {
      BaseIcuMessageConvertor(
        it,
        { NoOpFromIcuPlaceholderConvertor() },
      ).convert()
    }

  fun isPluralOptimized(): ImportTranslationInContextAssertions {
    assertTextNotNull()
    optimizePossiblePlural(this.translation.text!!).assert.isEqualTo(this.translation.text)
    return this
  }
}

fun FileProcessorContext.assertKey(
  keyName: String,
  fn: ImportKey.() -> Unit,
): ImportKey {
  val key = this.keys[keyName] ?: throw AssertionError("Key $keyName not found")
  fn(key)
  return key
}

val ImportKey.custom: Map<String, Any?>?
  get() = this.keyMeta?.custom

fun ImportKey.customEquals(expected: String) {
  val mapper = jacksonObjectMapper()
  val writer = mapper.writerWithDefaultPrettyPrinter()
  val expectedObject = mapper.readValue<Any?>(expected)
  val expectedString = writer.writeValueAsString(expectedObject)
  val actual = writer.writeValueAsString(custom)
  actual.assert.isEqualTo(expectedString)
}

val ImportKey.description: String?
  get() = this.keyMeta?.description
