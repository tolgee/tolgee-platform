package io.tolgee.repository.dataImport

import io.tolgee.model.dataImport.ImportKey
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ImportKeyRepository : JpaRepository<ImportKey, Long>
