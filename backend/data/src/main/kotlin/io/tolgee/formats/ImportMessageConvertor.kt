package io.tolgee.formats

interface ImportMessageConvertor {
  fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean = true,
    /**
     * This is used when Tolgee ICU placeholders are disabled globally on the project.
     * We need to forcefully escape everything to be able to de-escape everything when exporting.
     */
    forceEscapePluralForms: Boolean = false,
  ): MessageConvertorResult
}
