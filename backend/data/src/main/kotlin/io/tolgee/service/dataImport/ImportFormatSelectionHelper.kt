package io.tolgee.service.dataImport

import io.tolgee.formats.ImportMessageFormat
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile

class ImportFormatSelectionHelper(
  private val importDataManager: ImportDataManager,
  private val import: Import,
  private val file: ImportFile,
  private val format: ImportMessageFormat,
) {
  fun select() {
    file.languages.forEach {
      val storedTranslations = importDataManager.getStoredTranslations(it)
      TODO()
//      format.
    }
  }
}
