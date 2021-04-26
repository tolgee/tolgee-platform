package io.tolgee.repository.dataImport

import io.tolgee.model.Language
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.views.ImportLanguageView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.awt.print.Pageable

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
            count(il.translations) as totalCount, sum(case when it.collision is null then 1 else 0 end)
            from ImportLanguage il join il.file if join il.existingLanguage el join il.translations it
            group by il
            """)
    fun findImportLanguagesView(importId: Long, pageable: Pageable): List<ImportLanguageView>
}
