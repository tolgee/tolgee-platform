package io.tolgee.service.export

import io.tolgee.constants.Message
import io.tolgee.dtos.IExportParams
import io.tolgee.exceptions.BadRequestException
import io.tolgee.service.export.dataProvider.ExportTranslationView

class ExportFileStructureTemplateProvider(
  val params: IExportParams,
  translations: List<ExportTranslationView>,
) {
  fun validateAndGetTemplate(): String {
    validateTemplate()
    return getTemplate()
  }

  private fun getTemplate(): String {
    return params.fileStructureTemplate ?: params.format.defaultFileStructureTemplate
  }

  private fun validateTemplate() {
    validateLanguageTagInTemplate()
    validateExtensionInTemplate()
    validateNamespaceInTemplate()
  }

  /**
   * Validates the presence of the `{namespace}` placeholder in the file structure template
   * when multiple namespaces exist in the data to export. The presence of this placeholder is
   * mandatory to avoid filename collisions during the export process. Throws an exception
   * if the placeholder is missing.
   *
   * Throws:
   * - `BadRequestException`: If the `{namespace}` placeholder is missing from the template
   *   while the namespace count is greater than one.
   */
  private fun validateNamespaceInTemplate() {
    if (namespaceCount > 1) {
      // Not providing the namespace in the filename is not allowed and likely to cause collisions.
      if (!getTemplate().contains(ExportFilePathPlaceholder.NAMESPACE.placeholder)) {
        throw getMissingPlaceholderException(ExportFilePathPlaceholder.NAMESPACE)
      }
    }
  }

  private fun validateLanguageTagInTemplate() {
    if (!params.format.multiLanguage) {
      val containsLanguageTag =
        arrayOf(
          ExportFilePathPlaceholder.LANGUAGE_TAG,
          ExportFilePathPlaceholder.ANDROID_LANGUAGE_TAG,
          ExportFilePathPlaceholder.SNAKE_LANGUAGE_TAG,
        ).any { getTemplate().contains(it.placeholder) }

      if (!containsLanguageTag) {
        throw getMissingPlaceholderException(
          ExportFilePathPlaceholder.LANGUAGE_TAG,
          ExportFilePathPlaceholder.ANDROID_LANGUAGE_TAG,
          ExportFilePathPlaceholder.SNAKE_LANGUAGE_TAG,
        )
      }
    }
  }

  private fun validateExtensionInTemplate() {
    val containsExtension = getTemplate().contains(ExportFilePathPlaceholder.EXTENSION.placeholder)
    if (!containsExtension && params.format.fileStructureExtensionRequired) {
      throw getMissingPlaceholderException(ExportFilePathPlaceholder.EXTENSION)
    }
  }

  private fun getMissingPlaceholderException(vararg placeholder: ExportFilePathPlaceholder) =
    BadRequestException(
      Message.MISSING_PLACEHOLDER_IN_TEMPLATE,
      placeholder.toList(),
    )

  private val namespaceCount by lazy {
    translations.map { it.key.namespace }.distinct().size
  }
}
