package io.tolgee.repository.dataImport

import io.tolgee.model.Language
import io.tolgee.model.dataImport.ImportLanguage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ImportLanguageRepository : JpaRepository<ImportLanguage, Long> {
    @Query("from ImportLanguage il join il.file if join if.import im where im.id = :importId")
    fun findAllByImport(importId: Long): List<ImportLanguage>

    @Modifying
    @Query("update ImportLanguage il set il.existingLanguage = null where il.existingLanguage = :language")
    fun removeExistingLanguageReference(language: Language)
}
