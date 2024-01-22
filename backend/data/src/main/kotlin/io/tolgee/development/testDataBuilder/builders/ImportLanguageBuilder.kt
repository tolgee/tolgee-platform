package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.dataImport.ImportLanguage

class ImportLanguageBuilder(
  importFileBuilder: ImportFileBuilder,
) : EntityDataBuilder<ImportLanguage, ImportLanguageBuilder> {
  override var self: ImportLanguage = ImportLanguage("en", importFileBuilder.self)
}
