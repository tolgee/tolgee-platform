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
      and glossary.deletedAt is null
      and id = :id
  """,
  )
  fun find(
    organizationId: Long,
    glossaryId: Long,
    id: Long,
  ): GlossaryTerm?

  fun findByGlossary(glossary: Glossary): List<GlossaryTerm>

  @Query(
    """
    from GlossaryTerm te
    left join GlossaryTermTranslation tr on tr.term.id = te.id
      and tr.languageCode = te.glossary.baseLanguageCode
      and (:languageTags is null or tr.languageCode in :languageTags)
    where te.glossary.organizationOwner.id = :organizationId
      and te.glossary.organizationOwner.deletedAt is null
      and te.glossary.id = :glossaryId
      and te.glossary.deletedAt is null
      and (:search is null or
        lower(te.description) like lower(concat('%', cast(:search as text), '%')) or
        lower(tr.text) like lower(concat('%', cast(:search as text), '%'))
      )
  """,
  )
  fun findPaged(
    organizationId: Long,
    glossaryId: Long,
    pageable: Pageable,
    search: String?,
    languageTags: Set<String>?,
  ): Page<GlossaryTerm>

  @Query(
    """
    from GlossaryTerm te
    left join GlossaryTermTranslation tr on tr.term.id = te.id
      and tr.languageCode = te.glossary.baseLanguageCode
      and (:languageTags is null or tr.languageCode in :languageTags)
    where te.glossary = :glossary
      and (
        :search is null or
        lower(te.description) like lower(concat('%', cast(:search as text), '%')) or
        lower(tr.text) like lower(concat('%', cast(:search as text), '%'))
      )
  """,
  )
  fun findByGlossaryPaged(
    glossary: Glossary,
    pageable: Pageable,
    search: String?,
    languageTags: Set<String>?,
  ): Page<GlossaryTerm>

//  @Query(
//    """
//    select te.id as id,
//      te.description as description,
//      te.flagNonTranslatable as flagNonTranslatable,
//      te.flagCaseSensitive as flagCaseSensitive,
//      te.flagAbbreviation as flagAbbreviation,
//      te.flagForbiddenTerm as flagForbiddenTerm,
//      tr as translations
//    from GlossaryTerm te
//    left join GlossaryTermTranslation tr
//      on tr.term.id = te.id
//      and (:languageTags is null or tr.languageCode in :languageTags)
//    left join GlossaryTermTranslation tr_search
//      on tr_search.term.id = te.id
//      and (:languageTags is null or tr_search.languageCode in :languageTags)
//    where te.glossary = :glossary
//      and (
//        :search is null or
//        lower(te.description) like lower(concat('%', cast(:search as text), '%')) or
//        lower(tr_search.text) like lower(concat('%', cast(:search as text), '%'))
//      )
//  """,
//  )
// //  @Query(
// //    """
// //    select te.id as id,
// //      te.description as description,
// //      te.flagNonTranslatable as flagNonTranslatable,
// //      te.flagCaseSensitive as flagCaseSensitive,
// //      te.flagAbbreviation as flagAbbreviation,
// //      te.flagForbiddenTerm as flagForbiddenTerm,
// //      tr as translations
// //    from GlossaryTerm te
// //    left join GlossaryTermTranslation tr
// //      on tr.term.id = te.id
// //      and (:languageTags is null or tr.languageCode in :languageTags)
// //    where te.glossary = :glossary
// //      and (
// //        :search is null or
// //        lower(te.description) like lower(concat('%', cast(:search as text), '%')) or
// //        lower(tr.text) like lower(concat('%', cast(:search as text), '%'))
// //      )
// //  """,
// //  )
//  fun findByGlossaryPagedWithTranslations(
//    glossary: Glossary,
//    pageable: Pageable,
//    search: String?,
//    languageTags: Set<String>?,
//  ): Page<GlossaryTermWithTranslationsView>
}
