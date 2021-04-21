package io.tolgee.repository.dataImport

import io.tolgee.model.dataImport.ImportArchive
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImportArchiveRepository : JpaRepository<ImportArchive, Long>
