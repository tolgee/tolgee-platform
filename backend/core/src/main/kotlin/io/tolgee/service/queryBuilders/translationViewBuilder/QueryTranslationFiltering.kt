package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.TranslationFilterByLabel
import io.tolgee.dtos.request.translation.TranslationFilterByState
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Label_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.ListJoin
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate

class QueryTranslationFiltering(
  private val params: TranslationFilters,
  private val queryBase: QueryBase<*>,
  private val cb: CriteriaBuilder,
) {
  fun apply(
    language: LanguageDto,
    translationTextField: Path<String>,
    translationStateField: Path<TranslationState>,
    autoTranslatedField: Path<Boolean>,
  ) {
    filterByStateMap?.get(language.tag)?.let { states ->
      val languageStateConditions = mutableListOf<Predicate>()
      states.forEach { state ->
        var condition = cb.equal(translationStateField, state)
        if (state == TranslationState.UNTRANSLATED) {
          condition = cb.or(condition, cb.isNull(translationStateField))
        }
        languageStateConditions.add(condition)
      }
      queryBase.translationConditions.add(cb.or(*languageStateConditions.toTypedArray()))
    }

    if (params.filterAutoTranslatedInLang?.contains(language.tag) == true) {
      queryBase.translationConditions.add(cb.equal(autoTranslatedField, true))
    }

    if (params.filterUntranslatedInLang == language.tag) {
      queryBase.translationConditions.add(with(queryBase) { translationTextField.isNullOrBlank })
    }

    if (params.filterTranslatedInLang == language.tag) {
      queryBase.translationConditions.add(with(queryBase) { translationTextField.isNotNullOrBlank })
    }
  }

  fun apply(
    language: LanguageDto,
    commentsExpression: Expression<Long>,
    unresolvedCommentsExpression: Expression<Long>,
  ) {
    if (params.filterHasCommentsInLang?.contains(language.tag) == true) {
      queryBase.translationConditions.add(cb.greaterThan(commentsExpression, cb.literal(0L)))
    }

    if (params.filterHasUnresolvedCommentsInLang?.contains(language.tag) == true) {
      queryBase.translationConditions.add(cb.greaterThan(unresolvedCommentsExpression, cb.literal(0L)))
    }
  }

  fun apply(
    language: LanguageDto,
    commentsExpression: Expression<Long>,
  ) {
    if (params.filterHasSuggestionsInLang?.contains(language.tag) == true) {
      queryBase.translationConditions.add(cb.greaterThan(commentsExpression, cb.literal(0L)))
    }

    if (params.filterHasNoSuggestionsInLang?.contains(language.tag) == true) {
      queryBase.translationConditions.add(cb.equal(commentsExpression, cb.literal(0L)))
    }
  }

  fun apply(languageSourceChangeMap: MutableMap<String, Expression<Boolean>>) {
    val conditions =
      (
        params.filterOutdatedLanguage
          ?.mapNotNull {
            val field = languageSourceChangeMap[it] ?: return@mapNotNull null
            cb.isTrue(field)
          }?.toList() ?: listOf()
      ) + (
        params.filterNotOutdatedLanguage
          ?.mapNotNull {
            val field = languageSourceChangeMap[it] ?: return@mapNotNull null
            cb.isFalse(field)
          }?.toList() ?: listOf()
      )

    if (conditions.isNotEmpty()) {
      queryBase.translationConditions.add(cb.or(*conditions.toTypedArray()))
    }
  }

  fun apply(
    language: LanguageDto,
    translation: ListJoin<Key, Translation>,
  ) {
    filterByLabelMap?.get(language.tag)?.let { labelIds ->
      if (labelIds.isNotEmpty()) {
        val subquery = this.queryBase.query.subquery(Long::class.java)
        val subqueryRoot = subquery.from(Label::class.java)
        subquery.select(subqueryRoot.get(Label_.id))
        val conditions =
          labelIds.map { label ->
            cb.equal(subqueryRoot.get(Label_.id), label.labelId)
          }
        subquery.where(
          cb.and(
            cb.or(*conditions.toTypedArray()),
            cb.equal(
              subqueryRoot.join(Label_.translations).get(Translation_.id),
              translation.get(Translation_.id),
            ),
          ),
        )
        queryBase.translationConditions.add(cb.exists(subquery))
      }
    }
  }

  private val filterByStateMap: Map<String, List<TranslationState>>? by lazy {
    params.filterState
      ?.let { filterStateStrings -> TranslationFilterByState.parseList(filterStateStrings) }
      ?.let { filterByState ->
        val filterByStateMap = mutableMapOf<String, MutableList<TranslationState>>()

        filterByState.forEach {
          if (!filterByStateMap.containsKey(it.languageTag)) {
            filterByStateMap[it.languageTag] = mutableListOf()
          }
          filterByStateMap[it.languageTag]!!.add(it.state)
        }
        return@lazy filterByStateMap
      }
  }

  private val filterByLabelMap: Map<String, List<TranslationFilterByLabel>>? by lazy {
    params.filterLabel?.let { filterLabels ->
      TranslationFilterByLabel
        .parseList(filterLabels)
        .let { filterLabelsParsed ->
          val filterByLabelMap = mutableMapOf<String, MutableList<TranslationFilterByLabel>>()

          filterLabelsParsed.forEach {
            if (!filterByLabelMap.containsKey(it.languageTag)) {
              filterByLabelMap[it.languageTag] = mutableListOf()
            }
            filterByLabelMap[it.languageTag]!!.add(it)
          }
          return@lazy filterByLabelMap
        }
    }
  }
}
