package io.tolgee.repository.import

import io.tolgee.model.import.ImportArchive
import io.tolgee.model.import.ImportFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImportArchiveRepository : JpaRepository<ImportArchive, Long>
