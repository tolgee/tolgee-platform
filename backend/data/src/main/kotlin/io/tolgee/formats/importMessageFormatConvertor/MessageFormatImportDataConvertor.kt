package io.tolgee.formats.importMessageFormatConvertor

import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportTranslation

interface MessageFormatImportDataConvertor {
  fun convert(data: List<ImportTranslation>): StructuredRawDataConversionResult
}

data class StructuredRawDataConversionResult(
  val keysToRemove: List<ImportKey>,
  val keysToAdd: List<ImportKey>,
  val translationsToAdd: List<ImportTranslation>,
)
