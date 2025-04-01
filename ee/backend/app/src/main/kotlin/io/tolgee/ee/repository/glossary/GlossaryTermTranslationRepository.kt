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
    where t.term.glossary.id = :glossaryId
      and t.term.glossary.deletedAt is null
      and t.term.glossary.organizationOwner.id = :organizationId
      and t.term.glossary.organizationOwner.deletedAt is null
    """,
  )
  fun findDistinctLanguageTagsByGlossary(
    organizationId: Long,
    glossaryId: Long,
  ): Set<String>
}
