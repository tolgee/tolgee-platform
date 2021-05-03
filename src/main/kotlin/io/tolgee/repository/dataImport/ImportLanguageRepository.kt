package io.tolgee.repository.dataImport

import io.tolgee.model.Language
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.views.ImportLanguageView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface ImportLanguageRepository : JpaRepository<ImportLanguage, Long> {
    @Query("from ImportLanguage il join il.file if join if.import im where im.id = :importId")
    fun findAllByImport(importId: Long): List<ImportLanguage>

    @Modifying
    @Transactional
    @Query("update ImportLanguage il set il.existingLanguage = null where il.existingLanguage = :language")
    fun removeExistingLanguageReference(language: Language)

    @Query("""
            select il.id as id, il.name as name, el.id as existingLanguageId, 
            el.abbreviation as existingLanguageAbbreviation, el.name as existingLanguageName,
            if.name as importFileName, if.id as importFileId,
            count(it) as totalCount, 
            sum(case when it.conflict is null then 0 else 1 end) as conflictCount,
            sum(case when (it.conflict is null or it.resolved != true) then 0 else 1 end) as resolvedCount
            from ImportLanguage il join il.file if left join il.existingLanguage el left join il.translations it
            where if.import.id = :importId
            group by il.id
            """)
    fun findImportLanguagesView(importId: Long, pageable: Pageable): Page<ImportLanguageView>
}
