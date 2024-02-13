package io.tolgee.util

import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.NoOpFromIcuParamConvertor
import io.tolgee.formats.optimizePossiblePlural
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

fun FileProcessorContext.assertLanguagesCount(languagesCount: Int): FileProcessorContext {
  this.languages.size.assert.isEqualTo(languagesCount)
  return this
}

fun ImportTranslationInContextAssertions.hasKeyDescription(description: String) {
  val keyMeta =
    this.fileProcessorContext.keys[keyName]?.keyMeta
      ?: throw AssertionError("Key meta not found")
  keyMeta.description.assert.isEqualTo(description)
}

data class ImportTranslationInContextAssertions(
  val fileProcessorContext: FileProcessorContext,
  val translation: ImportTranslation,
  val keyName: String,
) {
  fun hasText(text: String): ImportTranslationInContextAssertions {
    this.translation.text.assert.isEqualTo(text)
    return this
  }

  fun assertIsPlural(): ImportTranslationInContextAssertions {
    this.assertTextNotNull()
    this.getPossiblePlural().isPlural()
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
        NoOpFromIcuParamConvertor(),
      ).convert()
    }

  fun assertHasExactPluralForms(forms: Set<String>): ImportTranslationInContextAssertions {
    this.getPossiblePlural().formsResult!!.keys.assert.isEqualTo(forms)
    return this
  }

  fun isPluralOptimized(): ImportTranslationInContextAssertions {
    assertTextNotNull()
    optimizePossiblePlural(this.translation.text!!).assert.isEqualTo(this.translation.text)
    return this
  }
}
