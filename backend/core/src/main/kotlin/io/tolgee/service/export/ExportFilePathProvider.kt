package io.tolgee.service.export

import com.ibm.icu.util.ULocale
import io.tolgee.service.export.dataProvider.ExportTranslationView

class ExportFilePathProvider(
  private val template: String,
  private val extension: String,
) {
  /**
   * This method takes the template provided in params and returns the file path by replacing the placeholders
   */
  fun getFilePath(
    namespace: String?,
    languageTag: String? = null,
    replaceExtension: Boolean = true,
  ): String {
    return template
      .replacePlaceholder(ExportFilePathPlaceholder.NAMESPACE, namespace ?: "")
      .replaceLanguageTag(languageTag ?: "all")
      .replaceExtensionIfEnabled(replaceExtension)
      .finalizePath()
  }

  fun replaceExtensionAndFinalize(path: String): String {
    return path
      .replaceExtensionIfEnabled(true)
      .finalizePath()
  }

  private fun String.finalizePath(): String {
    return this
      .replaceMultipleSlashes()
      .removeZipSlipString()
      .removeLeadingSlash()
  }

  private fun String.removeLeadingSlash(): String {
    return this.removePrefix("/")
  }

  private fun String.replaceMultipleSlashes() = this.replace(MULTIPLE_SLASHES, "/")

  private fun String.replacePlaceholder(
    placeholder: ExportFilePathPlaceholder,
    value: String,
  ) = this.replace(placeholder.placeholder, value)

  private fun String.replaceExtensionIfEnabled(replaceExtension: Boolean): String {
    if (replaceExtension) {
      val placeholder = ExportFilePathPlaceholder.EXTENSION

      return this
        .replace(
          "/+\\.${placeholder.placeholderForRegex}".toRegex(),
          ".${placeholder.placeholder}",
        ).replacePlaceholder(placeholder, extension)
    }
    return this
  }

  private fun String.replaceLanguageTag(languageTag: String): String {
    return this
      .replacePlaceholder(ExportFilePathPlaceholder.LANGUAGE_TAG, languageTag)
      .replacePlaceholder(
        ExportFilePathPlaceholder.ANDROID_LANGUAGE_TAG,
        convertBCP47ToAndroidResourceFormat(languageTag),
      ).replacePlaceholder(
        ExportFilePathPlaceholder.SNAKE_LANGUAGE_TAG,
        getSnakeLanguageTag(languageTag),
      )
  }

  fun getFilePath(exportTranslationView: ExportTranslationView): String {
    return getFilePath(exportTranslationView.key.namespace, exportTranslationView.languageTag)
  }

  fun getSnakeLanguageTag(languageTag: String) = languageTag.replace("-", "_")

  companion object {
    private val MULTIPLE_SLASHES = "/{2,}".toRegex()
    const val DEFAULT_TEMPLATE = "{namespace}/{languageTag}.{extension}"
  }

  private fun convertBCP47ToAndroidResourceFormat(languageTag: String): String {
    val uLocale = ULocale.forLanguageTag(languageTag)
    val language = uLocale.language
    val country = uLocale.country // assuming you have a region in your bcp47Tag

    return if (country.isEmpty()) {
      language
    } else {
      "$language-r$country"
    }
  }
}

private fun String.removeZipSlipString(): String {
  return this.replace("../", "")
}
