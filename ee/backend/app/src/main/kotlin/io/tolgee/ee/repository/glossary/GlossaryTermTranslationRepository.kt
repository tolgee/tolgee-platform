package io.tolgee.ee.repository.glossary

import io.tolgee.model.glossary.GlossaryTermTranslation
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface GlossaryTermTranslationRepository : JpaRepository<GlossaryTermTranslation, Long> {
  @Query(
    """
    select distinct t.languageCode
    from GlossaryTermTranslation t
    where t.glossaryTerm.glossary.id = :glossaryId
      and t.glossaryTerm.glossary.deletedAt is null
      and t.glossaryTerm.glossary.organizationOwner.id = :organizationId
      and t.glossaryTerm.glossary.organizationOwner.deletedAt is null
    """,
  )
  fun findDistinctLanguageTagsByGlossary(
    organizationId: Long,
    glossaryId: Long,
  ): Set<String>
}
