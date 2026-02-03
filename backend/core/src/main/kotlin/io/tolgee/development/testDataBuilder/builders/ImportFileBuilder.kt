package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation

class ImportFileBuilder(
  importBuilder: ImportBuilder,
) : BaseEntityDataBuilder<ImportFile, ImportFileBuilder>() {
  override var self: ImportFile = ImportFile("lang.json", importBuilder.self)

  class DATA {
    val importKeys = mutableListOf<ImportKeyBuilder>()
    val importLanguages = mutableListOf<ImportLanguageBuilder>()
    val importTranslations = mutableListOf<ImportTranslationBuilder>()
  }

  val data = DATA()

  fun addImportKey(ft: FT<ImportKey>) =
    addOperation(data.importKeys, ft).also {
      it.self {
        this@ImportFileBuilder.self.keys.add(this)
        this.file = this@ImportFileBuilder.self
      }
    }

  fun addImportLanguage(ft: FT<ImportLanguage>) =
    addOperation(data.importLanguages, ft).also {
      it.self { this.file = this@ImportFileBuilder.self }
    }

  fun addImportTranslation(ft: FT<ImportTranslation>) = addOperation(data.importTranslations, ft)
}
