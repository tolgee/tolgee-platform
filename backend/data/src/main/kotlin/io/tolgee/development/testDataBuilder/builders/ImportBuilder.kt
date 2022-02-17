package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile

class ImportBuilder(
  val projectBuilder: ProjectBuilder,
  author: UserAccount? = null
) : BaseEntityDataBuilder<Import, ImportBuilder>() {
  class DATA {
    val importFiles = mutableListOf<ImportFileBuilder>()
  }

  val data = DATA()

  override var self: Import = Import(author ?: projectBuilder.self.userOwner!!, projectBuilder.self)

  fun addImportFile(ft: FT<ImportFile>) = addOperation(data.importFiles, ft)
}
