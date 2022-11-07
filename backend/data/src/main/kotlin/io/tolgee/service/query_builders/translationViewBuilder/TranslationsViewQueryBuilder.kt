package io.tolgee.service.query_builders.translationViewBuilder

import io.tolgee.dtos.request.translation.TranslationFilterByState
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.dtos.response.CursorValue
import io.tolgee.model.*
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace_
import io.tolgee.model.key.Tag_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment_
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import org.springframework.data.domain.*
import java.util.*
import javax.persistence.criteria.*

class TranslationsViewQueryBuilder(
  private val cb: CriteriaBuilder,
  private val projectId: Long,
  private val languages: Set<Language>,
  private val params: TranslationFilters,
  private val sort: Sort,
  cursor: Map<String, CursorValue>? = null,
) {
  companion object {
    val KEY_NAME_FIELD = KeyWithTranslationsView::keyName.name
    val NAMESPACE_FIELD = KeyWithTranslationsView::keyNamespace.name
  }

  private val selection: LinkedHashMap<String, Selection<*>> = LinkedHashMap()
  private val cursorPredicateProvider = CursorPredicateProvider(cb, cursor, selection)

  private var fullTextFields: MutableSet<Expression<String>> = HashSet()
  private var whereConditions: MutableSet<Predicate> = HashSet()
  private lateinit var keyNameExpression: Path<String>
  private lateinit var namespaceNameExpression: Path<String>
  private lateinit var keyIdExpression: Path<Long>
  private var translationsTextFields: MutableSet<Expression<String>> = HashSet()
  private lateinit var root: Root<Key>
  private lateinit var screenshotCountExpression: Expression<Long>
  private val groupByExpressions: MutableSet<Expression<*>> = mutableSetOf()
  lateinit var query: CriteriaQuery<*>
  var isKeyIdsQuery = false

  private fun <T> getBaseQuery(query: CriteriaQuery<T>): CriteriaQuery<T> {
    this.query = query
    root = query.from(Key::class.java)
    keyIdExpression = root.get(Key_.id)
    selection[KeyWithTranslationsView::keyId.name] = keyIdExpression
    keyNameExpression = root.get(Key_.name)
    selection[KEY_NAME_FIELD] = keyNameExpression
    whereConditions.add(cb.equal(root.get<Any>(Key_.PROJECT).get<Any>(Project_.ID), this.projectId))
    fullTextFields.add(root.get(Key_.name))
    addLeftJoinedColumns()
    applyGlobalFilters()
    return query
  }

  private fun addLeftJoinedColumns() {
    addNamespace()
    addScreenshotCounts()
    selection[KeyWithTranslationsView::screenshotCount.name] = screenshotCountExpression
    for (language in languages) {
      val translation = root.join(Key_.translations, JoinType.LEFT)
      translation.on(cb.equal(translation.get(Translation_.language), language))
      val translationId = translation.get(Translation_.id)
      selection[KeyWithTranslationsView::translations.name + "." + language.tag + "." + TranslationView::id.name] =
        translationId
      groupByExpressions.add(translationId)
      val translationTextField = translation.get(Translation_.text)

      selection[KeyWithTranslationsView::translations.name + "." + language.tag + "." + TranslationView::text.name] =
        translationTextField
      val translationStateField = translation.get(Translation_.state)

      selection[KeyWithTranslationsView::translations.name + "." + language.tag + "." + TranslationView::state.name] =
        translationStateField

      addNotFilteringTranslationFields(language, translation)

      val commentsJoin = translation.join(Translation_.comments, JoinType.LEFT)
      val commentsExpression = cb.countDistinct(commentsJoin)
      selection[
        KeyWithTranslationsView::translations.name + "." + language.tag + "." + TranslationView::commentCount.name
      ] = commentsExpression

      val unresolvedCommentsJoin = translation.join(Translation_.comments, JoinType.LEFT)
      unresolvedCommentsJoin.on(
        cb.and(
          cb.equal(
            unresolvedCommentsJoin.get(TranslationComment_.translation),
            translation
          ),
          cb.equal(unresolvedCommentsJoin.get(TranslationComment_.state), TranslationCommentState.NEEDS_RESOLUTION)
        )
      )
      val unresolvedCommentsExpression = cb.countDistinct(unresolvedCommentsJoin)
      selection[
        KeyWithTranslationsView::translations.name + "." +
          language.tag + "." +
          TranslationView::unresolvedCommentCount.name
      ] = unresolvedCommentsExpression

      fullTextFields.add(translationTextField)
      translationsTextFields.add(translationTextField)
      applyTranslationFilters(language, translationTextField, translationStateField)
    }
  }

  private fun addScreenshotCounts() {
    val screenshotSubquery = query.subquery(Long::class.java)
    val screenshotRoot = screenshotSubquery.from(Screenshot::class.java)
    val screenshotCount = cb.count(screenshotRoot.get(Screenshot_.id))
    screenshotSubquery.select(screenshotCount)
    val screenshotsJoin: Join<Screenshot, Key> = screenshotRoot.join(Screenshot_.key)
    screenshotSubquery.where(cb.equal(root.get(Key_.id), screenshotsJoin.get(Key_.id)))
    screenshotCountExpression = screenshotSubquery.selection
  }

  private fun addNamespace() {
    val namespace = root.join(Key_.namespace, JoinType.LEFT)
    val namespaceName = namespace.get(Namespace_.name)
    namespaceNameExpression = namespaceName
    selection[NAMESPACE_FIELD] = namespaceName
    fullTextFields.add(namespaceName)
    groupByExpressions.add(namespaceName)
  }

  private fun addNotFilteringTranslationFields(
    language: Language,
    translation: SetJoin<Key, Translation>
  ) {
    if (!isKeyIdsQuery) {
      selection[KeyWithTranslationsView::translations.name + "." + language.tag + "." + TranslationView::auto.name] =
        translation.get(Translation_.auto)

      selection[
        KeyWithTranslationsView::translations.name + "." +
          language.tag + "." +
          TranslationView::mtProvider.name
      ] =
        translation.get(Translation_.mtProvider)
    }
  }

  private fun applyTranslationFilters(
    language: Language,
    translationTextField: Path<String>,
    translationStateField: Path<TranslationState>
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
      whereConditions.add(cb.or(*languageStateConditions.toTypedArray()))
    }

    if (params.filterUntranslatedInLang == language.tag) {
      whereConditions.add(translationTextField.isNullOrBlank)
    }
    if (params.filterTranslatedInLang == language.tag) {
      whereConditions.add(translationTextField.isNotNullOrBlank)
    }
  }

  private fun applyGlobalFilters() {
    if (params.filterTag != null) {
      val keyMetaJoin = root.join(Key_.keyMeta, JoinType.LEFT)
      val tagsJoin = keyMetaJoin.join(KeyMeta_.tags, JoinType.LEFT)
      whereConditions.add(tagsJoin.get(Tag_.name).`in`(params.filterTag))
    }
    if (params.filterKeyName != null) {
      whereConditions.add(keyNameExpression.`in`(params.filterKeyName))
    } else if (params.filterKeyId != null) {
      whereConditions.add(keyIdExpression.`in`(params.filterKeyId))
    } else {
      if (params.filterUntranslatedAny) {
        val predicates = translationsTextFields
          .map { it.isNullOrBlank }
          .toTypedArray()
        whereConditions.add(cb.or(*predicates))
      }
      if (params.filterTranslatedAny) {
        val predicates = translationsTextFields
          .map { it.isNotNullOrBlank }
          .toTypedArray()
        whereConditions.add(cb.or(*predicates))
      }
      if (params.filterHasScreenshot) {
        whereConditions.add(cb.gt(screenshotCountExpression, 0))
      }
      if (params.filterHasNoScreenshot) {
        whereConditions.add(cb.lt(screenshotCountExpression, 1))
      }

      val filterNamespace = params.filterNamespace
      if (filterNamespace !== null) {
        val inCondition = namespaceNameExpression.`in`(filterNamespace)
        val hasDefaultNamespace = filterNamespace.contains("")
        val condition = if (hasDefaultNamespace)
          cb.or(inCondition, namespaceNameExpression.isNull)
        else
          inCondition
        whereConditions.add(condition)
      }

      val search = params.search
      if (!search.isNullOrEmpty()) {
        val fullTextRestrictions: MutableSet<Predicate> = HashSet()
        for (fullTextField in fullTextFields) {
          fullTextRestrictions.add(
            cb.like(
              cb.upper(fullTextField),
              "%" + search.uppercase(Locale.getDefault()) + "%"
            )
          )
        }
        whereConditions.add(cb.or(*fullTextRestrictions.toTypedArray()))
      }
    }
  }

  val dataQuery: CriteriaQuery<Array<Any?>>
    get() {
      val query = getBaseQuery(cb.createQuery(Array<Any?>::class.java))
      val paths = selection.values.toTypedArray()
      query.multiselect(*paths)
      val orderList = sort.asSequence().filter { selection[it.property] != null }.map {
        val expression = selection[it.property] as Expression<*>
        when (it.direction) {
          Sort.Direction.DESC -> cb.desc(expression)
          else -> cb.asc(expression)
        }
      }.toMutableList()

      if (orderList.isEmpty()) {
        orderList.add(cb.asc(selection[KEY_NAME_FIELD] as Expression<*>))
      }
      val where = whereConditions.toMutableList()
      cursorPredicateProvider()?.let {
        where.add(it)
      }
      val groupBy = listOf(keyIdExpression, *groupByExpressions.toTypedArray())
      query.where(*where.toTypedArray())
      query.groupBy(groupBy)
      query.orderBy(orderList)
      return query
    }

  private val Expression<String>.isNotNullOrBlank get() = cb.and(cb.isNotNull(this), cb.notEqual(this, ""))

  private val Expression<String>.isNullOrBlank get() = cb.or(cb.isNull(this), cb.equal(this, ""))

  val countQuery: CriteriaQuery<Long>
    get() {
      val query = getBaseQuery(cb.createQuery(Long::class.java))
      val file = query.roots.iterator().next() as Root<*>
      query.select(cb.countDistinct(file))
      query.where(*whereConditions.toTypedArray())
      return query
    }

  val keyIdsQuery: CriteriaQuery<Long>
    get() {
      isKeyIdsQuery = true
      val query = getBaseQuery(cb.createQuery(Long::class.java))
      query.select(keyIdExpression)
      query.where(*whereConditions.toTypedArray())
      query.distinct(true)
      return query
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
}
