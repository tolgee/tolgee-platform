package io.tolgee.repository.dataImport

import io.tolgee.model.Translation
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.views.ImportTranslationView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface ImportTranslationRepository : JpaRepository<ImportTranslation, Long> {
    @Query("""
        select distinct it from ImportTranslation it 
        join it.language il on il.id = :languageId
        join il.file if
        join if.import i on i = :import
        """)
    fun findAllByImportAndLanguageId(import: Import, languageId: Long): List<ImportTranslation>

    @Modifying
    @Transactional
    @Query("update ImportTranslation it set it.conflict = null where it.conflict = :translation")
    fun removeExistingTranslationConflictReference(translation: Translation)


    @Query(""" select it.id as id, it.text as text, ik.name as keyName, ik.id as keyId,
        itc.id as conflictId, itc.text as conflictText, it.override as override, it.resolved as resolved
        from ImportTranslation it left join it.conflict itc join it.key ik
        where (itc.id is not null or :onlyConflicts = false)
        and ((itc.id is not null and it.resolved = false) or :onlyUnresolved = false)
        and it.language.id = :languageId
    """)
    fun findImportTranslationsView(languageId: Long,
                                   pageable: Pageable,
                                   onlyConflicts: Boolean = false,
                                   onlyUnresolved: Boolean = false
    ): Page<ImportTranslationView>
}
