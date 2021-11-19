package io.tolgee.repository.dataImport

import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImportFileRepository : JpaRepository<ImportFile, Long> {
  fun deleteAllByImport(import: Import)
}
