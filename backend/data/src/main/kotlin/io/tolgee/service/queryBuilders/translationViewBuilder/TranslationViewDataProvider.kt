package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.service.key.TagService
import io.tolgee.service.queryBuilders.CursorUtil
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class TranslationViewDataProvider(
  private val em: EntityManager,
  private val tagService: TagService,
) {
  fun getData(
    projectId: Long,
    languages: Set<LanguageDto>,
    pageable: Pageable,
    params: TranslationFilters = TranslationFilters(),
    cursor: String? = null,
  ): Page<KeyWithTranslationsView> {
    // otherwise it takes forever for postgres to plan the execution
    em.createNativeQuery("SET join_collapse_limit TO 1").executeUpdate()

    var translationsViewQueryBuilder = getTranslationsViewQueryBuilder(projectId, languages, params, pageable, cursor)
    val count = em.createQuery(translationsViewQueryBuilder.countQuery).singleResult

    translationsViewQueryBuilder = getTranslationsViewQueryBuilder(projectId, languages, params, pageable, cursor)
    val query = em.createQuery(translationsViewQueryBuilder.dataQuery).setMaxResults(pageable.pageSize)
    if (cursor == null) {
      query.firstResult = pageable.offset.toInt()
    }
    val views = query.resultList.map { KeyWithTranslationsView.of(it, languages.toList()) }

    // reset the value
    em.createNativeQuery("SET join_collapse_limit TO DEFAULT").executeUpdate()

    val keyIds = views.map { it.keyId }
    tagService.getTagsForKeyIds(keyIds).let { tagMap ->
      views.forEach { it.keyTags = tagMap[it.keyId] ?: listOf() }
    }
    return PageImpl(views, pageable, count)
  }

  fun getSelectAllKeys(
    projectId: Long,
    languages: Set<LanguageDto>,
    params: TranslationFilters = TranslationFilters(),
  ): MutableList<Long> {
    val translationsViewQueryBuilder =
      TranslationsViewQueryBuilder(
        cb = em.criteriaBuilder,
        projectId = projectId,
        languages = languages,
        params = params,
        sort = Sort.by(Sort.Order.asc(KeyWithTranslationsView::keyId.name)),
      )
    return em.createQuery(translationsViewQueryBuilder.keyIdsQuery).resultList
  }

  private fun getTranslationsViewQueryBuilder(
    projectId: Long,
    languages: Set<LanguageDto>,
    params: TranslationFilters,
    pageable: Pageable,
    cursor: String?,
  ) = TranslationsViewQueryBuilder(
    cb = em.criteriaBuilder,
    projectId = projectId,
    languages = languages,
    params = params,
    sort = pageable.sort,
    cursor = cursor?.let { CursorUtil.parseCursor(it) },
  )
}
