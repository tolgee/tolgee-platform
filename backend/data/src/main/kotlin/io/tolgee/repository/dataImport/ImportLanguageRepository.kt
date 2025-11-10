package io.tolgee.repository.dataImport

import io.tolgee.model.Language
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.views.ImportLanguageView
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Repository
@Lazy
interface ImportLanguageRepository : JpaRepository<ImportLanguage, Long> {
  companion object {
    private const val VIEW_BASE_QUERY = """
    SELECT 
    il.id AS id, 
    il.name AS name, 
    el.id AS existingLanguageId, 
    el.tag AS existingLanguageTag, 
    el.name AS existingLanguageName, 
    f.name AS importFileName, 
    f.id AS importFileId, 
    f.namespace AS namespace, 
    (SELECT COUNT(i.id) FROM ImportFileIssue i WHERE f.id = i.file.id) AS importFileIssueCount, 
    (SELECT COUNT(t.id) FROM ImportTranslation t WHERE t.language.id = il.id AND t.isSelectedToImport = TRUE
and t.key.shouldBeImported
    ) AS totalCount, 
    COALESCE ((SELECT SUM(CASE WHEN t.conflict IS NULL THEN 0 ELSE 1 END) FROM ImportTranslation t WHERE t.language.id = il.id AND t.isSelectedToImport = TRUE 
    and t.key.shouldBeImported
    ),0) AS conflictCount, 
    COALESCE ((SELECT SUM(CASE WHEN t.conflict IS NULL OR t.resolvedHash IS NULL THEN 0 ELSE 1 END) FROM ImportTranslation t WHERE t.language.id = il.id AND t.isSelectedToImport = TRUE
and t.key.shouldBeImported
    ), 0) AS resolvedCount 
FROM ImportLanguage il 
JOIN il.file f 
LEFT JOIN il.existingLanguage el 

        """

    private const val VIEW_GROUP_BY = """
            group by il.id, f.id, el.id
        """
  }

  @Query("from ImportLanguage il join il.file if join if.import im where im.id = :importId")
  fun findAllByImport(importId: Long): List<ImportLanguage>

  @Modifying
  @Transactional
  @Query("update ImportLanguage il set il.existingLanguage = null where il.existingLanguage = :language")
  fun removeExistingLanguageReference(language: Language)

  @Query(
    """
   $VIEW_BASE_QUERY
        WHERE f.import.id = :importId
            $VIEW_GROUP_BY
            order by il.id
            """,
  )
  fun findImportLanguagesView(
    importId: Long,
    pageable: Pageable,
  ): Page<ImportLanguageView>

  @Query(
    """
      $VIEW_BASE_QUERY
      where (il.id = :languageId)
            $VIEW_GROUP_BY
            """,
  )
  fun findViewById(languageId: Long): Optional<ImportLanguageView>
}
