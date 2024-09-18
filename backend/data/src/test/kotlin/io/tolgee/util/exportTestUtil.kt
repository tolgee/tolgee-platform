package io.tolgee.util

import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView

fun buildExportTranslation(
  languageTag: String,
  keyName: String,
  text: String?,
  description: String? = null,
  fn: (ExportTranslationView.() -> Unit)? = null,
): ExportTranslationView {
  val translation =
    ExportTranslationView(
      id = null,
      text = text,
      key = ExportKeyView(name = keyName),
      languageTag = languageTag,
      description = description,
    )
  fn?.invoke(translation)
  return translation
}

fun buildExportTranslationList(fn: BuildExportTranslationListContext.() -> Unit): BuildExportTranslationListContext {
  val context = BuildExportTranslationListContext()
  fn(context)
  return context
}

class BuildExportTranslationListContext(
  val baseTranslations: MutableList<ExportTranslationView> = mutableListOf(),
  val translations: MutableList<ExportTranslationView> = mutableListOf(),
  private val baseLanguageTag: String = "en",
) {
  fun add(
    languageTag: String,
    keyName: String,
    text: String?,
    baseText: String? = null,
    description: String? = null,
    fn: (BuildExportTranslationContext.() -> Unit)? = null,
  ): BuildExportTranslationListContext {
    val translation = buildExportTranslation(languageTag, keyName, text, description)
    val baseTranslation =
      ExportTranslationView(
        id = null,
        text = baseText,
        key = translation.key,
        languageTag = baseLanguageTag,
        description = description,
      )
    baseTranslations.add(baseTranslation)
    translations.add(translation)
    val context =
      BuildExportTranslationContext(translation, baseTranslation, baseTranslations, translations, baseLanguageTag)
    fn?.invoke(context)
    return this
  }
}

class BuildExportTranslationContext(
  val translation: ExportTranslationView,
  val baseTranslation: ExportTranslationView,
  private val baseTranslations: MutableList<ExportTranslationView>,
  private val translations: MutableList<ExportTranslationView>,
  private val baseLanguageTag: String,
) {
  val key get() = translation.key
}
