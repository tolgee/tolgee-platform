package io.tolgee.ee.repository.glossary

import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface GlossaryTermTranslationRepository : JpaRepository<GlossaryTermTranslation, Long> {
  @Query(
    """
    select distinct t.languageTag
    from GlossaryTermTranslation t
    where t.term.glossary.id = :glossaryId
      and t.term.glossary.organizationOwner.id = :organizationId
      and t.term.glossary.organizationOwner.deletedAt is null
    order by t.languageTag asc
    """,
  )
  fun findDistinctLanguageTagsByGlossary(
    organizationId: Long,
    glossaryId: Long,
  ): Set<String>

  @Query(
    """
      from GlossaryTermTranslation gtt
      where
        gtt.term = :term and
        (gtt.languageTag = :languageTag or gtt.term.flagNonTranslatable)
    """,
  )
  fun findByTermAndLanguageTag(
    term: GlossaryTerm,
    languageTag: String,
  ): GlossaryTermTranslation?

  fun deleteByTermAndLanguageTag(
    term: GlossaryTerm,
    languageTag: String,
  )

  fun deleteAllByTermAndLanguageTagIsNot(
    term: GlossaryTerm,
    languageTag: String,
  )

  @Query(
    """
      from GlossaryTermTranslation gtt
        join gtt.term.glossary.assignedProjects ap
        where
            gtt.firstWordLowercased in :texts and
            (gtt.languageTag = :languageTag or gtt.term.flagNonTranslatable) and
            ap.id = :assignedProjectId and
            gtt.term.glossary.organizationOwner.id = :organizationId
    """,
  )
  fun findByFirstWordLowercased(
    texts: Collection<String>,
    languageTag: String,
    assignedProjectId: Long,
    organizationId: Long,
  ): Set<GlossaryTermTranslation>

  @Modifying
  @Query(
    """
      update GlossaryTermTranslation gtt
      set gtt.languageTag = :newBaseLanguageTag
      where gtt.term.glossary = :glossary
       and gtt.languageTag = :oldBaseLanguageTag
       and gtt.term.flagNonTranslatable = true
    """,
  )
  fun updateBaseLanguage(
    glossary: Glossary,
    oldBaseLanguageTag: String?,
    newBaseLanguageTag: String?,
  )
}
