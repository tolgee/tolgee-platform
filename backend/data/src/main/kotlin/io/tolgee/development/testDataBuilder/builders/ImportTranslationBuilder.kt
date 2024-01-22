package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.dataImport.ImportTranslation

class ImportTranslationBuilder(
  importFileBuilder: ImportFileBuilder,
) : EntityDataBuilder<ImportTranslation, ImportTranslationBuilder> {
  override var self: ImportTranslation =
    ImportTranslation("test translation", importFileBuilder.data.importLanguages[0].self).apply {
      importFileBuilder.data.importKeys.firstOrNull()?.self?.let {
        key = it
      }
    }
}
