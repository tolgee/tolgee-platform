package io.tolgee.service.query_builders

import io.tolgee.dtos.request.translation.TranslationFilterByState
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.dtos.response.CursorValue
import io.tolgee.model.*
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Tag_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment_
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import io.tolgee.service.TagService
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.*
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.criteria.*

class TranslationsViewBuilder(
  private val cb: CriteriaBuilder,
  private val projectId: Long,
  private val languages: Set<Language>,
  private val params: TranslationFilters,
  private val sort: Sort,
  private val cursor: Map<String, CursorValue>? = null,
) {
  private var selection: LinkedHashMap<String, Selection<*>> = LinkedHashMap()
  private var fullTextFields: MutableSet<Expression<String>> = HashSet()
  private var whereConditions: MutableSet<Predicate> = HashSet()
  private lateinit var keyNameExpression: Path<String>
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

  @Suppress("UNCHECKED_CAST", "TYPE_MISMATCH_WARNING")
  /**
   * This function body is inspired by this thread
   * https://stackoverflow.com/questions/38017054/mysql-cursor-based-pagination-with-multiple-columns
   */
  private fun getCursorPredicate(): Predicate? {
    var result: Predicate? = null
    cursor?.entries?.reversed()?.forEach { (property, value) ->
      val isUnique = property === KeyWithTranslationsView::keyId.name
      val expression = selection[property] as Expression<String>

      val strongCondition: Predicate
      val condition: Predicate
      if (value.direction == Sort.Direction.ASC) {
        condition = if (isUnique)
          cb.greaterThan(expression, value.value)
        else
          cb.greaterThanOrEqualToNullable(expression, value.value)
        strongCondition = cb.greaterThan(expression, value.value)
      } else {
        condition = if (isUnique)
          cb.lessThan(expression, value.value)
        else
          cb.lessThanOrEqualToNullable(expression, value.value)
        strongCondition = cb.lessThan(expression, value.value)
      }
      result = result?.let {
        cb.and(condition, cb.or(strongCondition, result))
      } ?: condition
    }
    return result
  }

  @Suppress("TYPE_MISMATCH_WARNING")
  private fun CriteriaBuilder.greaterThanOrEqualToNullable(
    expression: Expression<String>,
    value: String?
  ): Predicate {
    if (value == null) {
      return this.or(this.isNull(expression), this.greaterThan(expression, value as String?))
    }
    return this.greaterThanOrEqualTo(expression, value as String?)
  }

  @Suppress("TYPE_MISMATCH_WARNING")
  private fun CriteriaBuilder.lessThanOrEqualToNullable(
    expression: Expression<String>,
    value: String?
  ): Predicate {
    if (value == null) {
      return this.or(this.isNull(expression), this.lessThan(expression, value as String?))
    }
    return this.lessThanOrEqualTo(expression, value as String?)
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

  private fun addLeftJoinedColumns() {
    addScreenshotCounts()
    selection[KeyWithTranslationsView::screenshotCount.name] = screenshotCountExpression
    val project = root.join(Key_.project)
    for (language in languages) {
      val languagesJoin = project.join(Project_.languages)
      languagesJoin.on(cb.equal(languagesJoin.get(Language_.tag), language.tag))
      val translation = root.join(Key_.translations, JoinType.LEFT)
      translation.on(cb.equal(translation.get(Translation_.language), languagesJoin))
      val languageTag = languagesJoin.get(Language_.tag)
      selection[KeyWithTranslationsView::translations.name + "." + language.tag] = languageTag
      groupByExpressions.add(languageTag)
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
      whereConditions.add(cb.equal(keyNameExpression, params.filterKeyName))
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

  @Suppress("UNCHECKED_CAST")
  private val dataQuery: CriteriaQuery<Array<Any?>>
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
      getCursorPredicate()?.let {
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

  private val countQuery: CriteriaQuery<Long>
    get() {
      val query = getBaseQuery(cb.createQuery(Long::class.java))
      val file = query.roots.iterator().next() as Root<*>
      query.select(cb.countDistinct(file))
      query.where(*whereConditions.toTypedArray())
      return query
    }

  private val keyIdsQuery: CriteriaQuery<Long>
    get() {
      isKeyIdsQuery = true
      val query = getBaseQuery(cb.createQuery(Long::class.java))
      query.select(keyIdExpression)
      query.where(*whereConditions.toTypedArray())
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

  companion object {
    val KEY_NAME_FIELD = KeyWithTranslationsView::keyName.name

    @JvmStatic
    fun getData(
      applicationContext: ApplicationContext,
      projectId: Long,
      languages: Set<Language>,
      pageable: Pageable,
      params: TranslationFilters = TranslationFilters(),
      cursor: String? = null
    ): Page<KeyWithTranslationsView> {
      val em = applicationContext.getBean(EntityManager::class.java)
      val tagService = applicationContext.getBean(TagService::class.java)
      var sort = if (pageable.sort.isSorted)
        pageable.sort
      else
        Sort.by(Sort.Order.asc(KEY_NAME_FIELD))

      sort = sort.and(Sort.by(Sort.Direction.ASC, KeyWithTranslationsView::keyId.name))

      var translationsViewBuilder = TranslationsViewBuilder(
        cb = em.criteriaBuilder,
        projectId = projectId,
        languages = languages,
        params = params,
        sort = sort,
        cursor = cursor?.let { CursorUtil.parseCursor(it) }
      )
      val count = em.createQuery(translationsViewBuilder.countQuery).singleResult

      translationsViewBuilder = TranslationsViewBuilder(
        cb = em.criteriaBuilder,
        projectId = projectId,
        languages = languages,
        params = params,
        sort = sort,
        cursor = cursor?.let { CursorUtil.parseCursor(it) }
      )
      val query = em.createQuery(translationsViewBuilder.dataQuery).setMaxResults(pageable.pageSize)
      if (cursor == null) {
        query.firstResult = pageable.offset.toInt()
      }
      val views = query.resultList.map { KeyWithTranslationsView.of(it) }
      val keyIds = views.map { it.keyId }
      tagService.getTagsForKeyIds(keyIds).let { tagMap ->
        views.forEach { it.keyTags = tagMap[it.keyId] ?: listOf() }
      }
      return PageImpl(views, pageable, count)
    }

    fun getSelectAllKeys(
      applicationContext: ApplicationContext,
      projectId: Long,
      languages: Set<Language>,
      params: TranslationFilters = TranslationFilters(),
    ): MutableList<Long> {
      val em = applicationContext.getBean(EntityManager::class.java)

      val translationsViewBuilder = TranslationsViewBuilder(
        cb = em.criteriaBuilder,
        projectId = projectId,
        languages = languages,
        params = params,
        sort = Sort.by(Sort.Order.asc(KeyWithTranslationsView::keyId.name))
      )
      return em.createQuery(translationsViewBuilder.keyIdsQuery).resultList
    }
  }
}
