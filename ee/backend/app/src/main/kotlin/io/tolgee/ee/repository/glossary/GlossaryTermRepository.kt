package io.tolgee.ee.repository.glossary

import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface GlossaryTermRepository : JpaRepository<GlossaryTerm, Long> {
  @Query(
    """
    from GlossaryTerm
    where glossary.organizationOwner.id = :organizationId
      and glossary.organizationOwner.deletedAt is null
      and glossary.id = :glossaryId
      and id = :id
  """,
  )
  fun find(
    organizationId: Long,
    glossaryId: Long,
    id: Long,
  ): GlossaryTerm?

  fun findByGlossary(glossary: Glossary): List<GlossaryTerm>

  fun deleteAllByGlossary(glossary: Glossary)

  @Query(
    """
    from GlossaryTerm te
    left join GlossaryTermTranslation tr on tr.term.id = te.id
      and tr.languageTag = te.glossary.baseLanguageTag
      and (:languageTags is null or tr.languageTag in :languageTags)
    where te.glossary = :glossary
      and (
        lower(te.description) like lower(concat('%', coalesce(:search, ''), '%')) or
        lower(tr.text) like lower(concat('%', coalesce(:search, ''), '%')) or
        :search is null
      )
  """,
  )
  fun findByGlossaryPaged(
    glossary: Glossary,
    pageable: Pageable,
    search: String?,
    languageTags: Set<String>?,
  ): Page<GlossaryTerm>

  @Query(
    """
    select te.id
    from GlossaryTerm te
    left join GlossaryTermTranslation tr on tr.term.id = te.id
      and tr.languageTag = te.glossary.baseLanguageTag
      and (:languageTags is null or tr.languageTag in :languageTags)
    where te.glossary = :glossary
      and (
        lower(te.description) like lower(concat('%', coalesce(:search, ''), '%')) or
        lower(tr.text) like lower(concat('%', coalesce(:search, ''), '%')) or
        :search is null
      )
  """,
  )
  fun findByGlossaryIdsPaged(
    glossary: Glossary,
    pageable: Pageable,
    search: String?,
    languageTags: Set<String>?,
  ): Page<Long>

  @Query(
    """
    from GlossaryTerm te
    left join fetch te.translations
    where te.id in :ids
  """,
  )
  fun findByIdsWithTranslations(ids: Collection<Long>): List<GlossaryTerm>

  @Query(
    """
    from GlossaryTerm te
    left join fetch te.translations
    where te.glossary = :glossary
  """,
  )
  fun findByGlossaryWithTranslations(glossary: Glossary): List<GlossaryTerm>

  @Query(
    """
    select te.id from GlossaryTerm te
    left join GlossaryTermTranslation tr on tr.term.id = te.id
      and tr.languageTag = te.glossary.baseLanguageTag
      and (:languageTags is null or tr.languageTag in :languageTags)
    where te.glossary = :glossary
      and (
        lower(te.description) like lower(concat('%', coalesce(:search, ''), '%')) or
        lower(tr.text) like lower(concat('%', coalesce(:search, ''), '%')) or
        :search is null
      )
  """,
  )
  fun findAllIds(
    glossary: Glossary,
    search: String?,
    languageTags: Set<String>?,
  ): List<Long>

  fun deleteByGlossaryAndIdIn(
    glossary: Glossary,
    ids: Collection<Long>,
  )
}
