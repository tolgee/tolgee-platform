package io.tolgee.repository.dataImport

import io.tolgee.model.Language
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.views.ImportLanguageView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface ImportLanguageRepository : JpaRepository<ImportLanguage, Long> {

  companion object {
    private const val VIEW_BASE_QUERY = """
            select il.id as id, il.name as name, el.id as existingLanguageId, 
            el.tag as existingLanguageTag, el.name as existingLanguageName,
            if.name as importFileName, if.id as importFileId,
            if.namespace as namespace,
            (select count(*) from if.issues) as importFileIssueCount,
            count(it) as totalCount, 
            sum(case when it.conflict is null then 0 else 1 end) as conflictCount,
            sum(case when (it.conflict is null or it.resolvedHash is null) then 0 else 1 end) as resolvedCount
            from ImportLanguage il join il.file if left join il.existingLanguage el left join il.translations it
        """

    private const val VIEW_GROUP_BY = """
            group by il.id, if.id, el.id
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
            where if.import.id = :importId
            $VIEW_GROUP_BY
            order by il.id
            """
  )
  fun findImportLanguagesView(importId: Long, pageable: Pageable): Page<ImportLanguageView>

  @Modifying
  @Transactional
  @Query(
    """delete from ImportLanguage l where l.file in 
        (select f from ImportFile f where f.import = :import)"""
  )
  fun deleteAllByImport(import: Import)

  @Query(
    """
            $VIEW_BASE_QUERY
            where il.id = :languageId
            $VIEW_GROUP_BY
            """
  )
  fun findViewById(languageId: Long): Optional<ImportLanguageView>

  @Query(
    """
      select distinct il.existingLanguage.id 
        from ImportLanguage il 
        join il.file if 
        where if.import.id = :importId 
          and il.existingLanguage.id is not null
    """
  )
  fun findAssignedExistingLanguageIds(importId: Long): List<Long>
}
