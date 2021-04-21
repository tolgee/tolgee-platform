package io.tolgee.repository.dataImport

import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportTranslation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ImportTranslationRepository : JpaRepository<ImportTranslation, Long> {
    @Query("""
        select distinct it from ImportTranslation it 
        join it.language il on il.id = :languageId
        join il.file if
        join if.import i on i = :import
        """)
    fun findAllByImportAndLanguageId(import: Import, languageId: Long): List<ImportTranslation>
}
