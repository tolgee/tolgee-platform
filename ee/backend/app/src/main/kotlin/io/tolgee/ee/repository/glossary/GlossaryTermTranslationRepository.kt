package io.tolgee.ee.repository.glossary

import io.tolgee.model.Project
import io.tolgee.model.glossary.GlossaryTerm
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
    select distinct t.languageTag
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

  fun findByTermAndLanguageTag(
    term: GlossaryTerm,
    languageTag: String,
  ): GlossaryTermTranslation?

  fun deleteByTermAndLanguageTag(
    term: GlossaryTerm,
    languageTag: String,
  )

  @Query(
    """
      from GlossaryTermTranslation g
        where
            g.textLowercased in :texts and
            g.languageTag = :languageTag and
            :assignedProject member of g.term.glossary.assignedProjects and
            g.term.glossary.deletedAt is null
    """,
  )
  fun findByLowercaseTextAndAssignedProject(
    texts: Collection<String>,
    languageTag: String,
    assignedProject: Project,
  ): Set<GlossaryTermTranslation>
}
