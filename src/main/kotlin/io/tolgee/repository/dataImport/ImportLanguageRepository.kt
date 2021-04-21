package io.tolgee.repository.dataImport

import io.tolgee.model.dataImport.ImportLanguage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ImportLanguageRepository : JpaRepository<ImportLanguage, Long> {
    @Query("from ImportLanguage il join il.file if join if.import im where im.id = :importId")
    fun findAllByImport(importId: Long): List<ImportLanguage>
}
