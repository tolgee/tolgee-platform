package io.tolgee.service.export

import com.ibm.icu.util.ULocale
import io.tolgee.dtos.IExportParams
import io.tolgee.service.export.dataProvider.ExportTranslationView

class ExportFilePathProvider(
  private val params: IExportParams,
  private val extension: String,
  private val defaultTemplate: String = DEFAULT_PATH,
) {
  /**
   * This method takes the template provided in params and returns the file path by replacing the placeholders
   */
  fun getFilePath(
    namespace: String?,
    languageTag: String,
  ): String {
    val template = params.fileStructureTemplate ?: defaultTemplate
    return template
      .replace("{namespace}", namespace ?: "")
      .replace("{languageTag}", languageTag)
      .replace("{androidLanguageTag}", convertBCP47ToAndroidResourceFormat(languageTag))
      .replace("{snakeLanguageTag}", convertBCP47ToAndroidResourceFormat(getSnakeLanguageTag(languageTag)))
      .replace("{extension}", extension)
      .replace(MULTIPLE_SLASHES, "/")
  }

  fun getFilePath(exportTranslationView: ExportTranslationView): String {
    return getFilePath(exportTranslationView.key.namespace, exportTranslationView.languageTag)
  }

  fun getSnakeLanguageTag(languageTag: String) = languageTag.replace("-", "_")

  companion object {
    private val MULTIPLE_SLASHES = "/{2,}".toRegex()
    private const val DEFAULT_PATH = "{namespace}/{languageTag}.{extension}"
  }

  private fun convertBCP47ToAndroidResourceFormat(languageTag: String): String {
    val uLocale = ULocale.forLanguageTag(languageTag)
    val language = uLocale.language
    val country = uLocale.country // assuming you have a region in your bcp47Tag

    return if (country.isEmpty()) {
      "values-$language"
    } else {
      "values-$language-r$country"
    }
  }
}
