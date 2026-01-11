package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile

class ImportBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<Import, ImportBuilder>() {
  class DATA {
    val importFiles = mutableListOf<ImportFileBuilder>()
  }

  val data = DATA()

  override var self: Import =
    Import(projectBuilder.self).apply {
      projectBuilder.onlyUser?.let {
        author = it
      }
    }

  fun addImportFile(ft: FT<ImportFile>) = addOperation(data.importFiles, ft)
}
