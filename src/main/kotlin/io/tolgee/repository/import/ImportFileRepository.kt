package io.tolgee.repository.import

import io.tolgee.model.import.ImportFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImportFileRepository : JpaRepository<ImportFile, Long>
