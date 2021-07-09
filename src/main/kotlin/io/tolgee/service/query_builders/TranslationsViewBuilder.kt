package io.tolgee.service.query_builders

import io.tolgee.dtos.request.GetTranslationsParams
import io.tolgee.dtos.response.CursorValue
import io.tolgee.model.*
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import org.springframework.data.domain.*
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.criteria.*

class TranslationsViewBuilder(
  private val cb: CriteriaBuilder,
  private val project: Project?,
  private val languages: Set<Language>,
  private val params: GetTranslationsParams,
  private val sort: Sort,
  private val cursor: Map<String, CursorValue>?,
) {
  private var selection: LinkedHashMap<String, Selection<*>> = LinkedHashMap()
  private var fullTextFields: MutableSet<Expression<String>> = HashSet()
  private var whereConditions: MutableSet<Predicate> = HashSet()
  private lateinit var keyNameExpression: Path<String>
  private lateinit var keyIdExpression: Path<Long>
  private var translationsTextFields: MutableSet<Expression<String>> = HashSet()
  private lateinit var root: Root<Key>
  private lateinit var screenshotCountExpression: Expression<Long>
  private val havingConditions: MutableSet<Predicate> = HashSet()
  private val groupByExpressions: MutableSet<Expression<*>> = mutableSetOf()

  private fun <T> getBaseQuery(query: CriteriaQuery<T>): CriteriaQuery<T> {
    root = query.from(Key::class.java)
    keyIdExpression = root.get(Key_.id)
    selection[KeyWithTranslationsView::keyId.name] = keyIdExpression
    keyNameExpression = root.get(Key_.name)
    selection[KEY_NAME_FIELD] = keyNameExpression
    whereConditions.add(cb.equal(root.get<Any>(Key_.PROJECT), this.project))
    fullTextFields.add(root.get(Key_.name))
    addLeftJoinedColumns()
    applyGlobalFilters()
    query.where(*whereConditions.toTypedArray())
    query.having(*havingConditions.toTypedArray())
    return query
  }

  @Suppress("UNCHECKED_CAST")
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
          cb.greaterThan(expression, value.value as String)
        else
          cb.greaterThanOrEqualTo(expression, value.value as String)
        strongCondition = cb.greaterThan(expression, value.value)
      } else {
        condition = if (isUnique)
          cb.lessThan(expression, value.value as String)
        else
          cb.lessThanOrEqualTo(expression, value.value as String)
        strongCondition = cb.lessThan(expression, value.value)
      }
      result = result?.let {
        cb.and(condition, cb.or(strongCondition, result))
      } ?: condition
    }
    return result
  }

  private fun addLeftJoinedColumns() {
    val screenshotsJoin = root.join(Key_.screenshots, JoinType.LEFT)
    screenshotCountExpression = cb.count(screenshotsJoin)
    selection[KeyWithTranslationsView::screenshotCount.name] = screenshotCountExpression
    val project = root.join(Key_.project)
    for (language in languages) {
      val languagesJoin = project.join(Project_.languages)
      languagesJoin.on(cb.equal(languagesJoin.get(Language_.tag), language.tag))
      val translations = root.join(Key_.translations, JoinType.LEFT)
      translations.on(cb.equal(translations.get(Translation_.language), languagesJoin))
      val languageTag = languagesJoin.get(Language_.tag)
      selection[KeyWithTranslationsView::translations.name + "." + language.tag] = languageTag
      groupByExpressions.add(languageTag)
      val translationId = translations.get(Translation_.id)
      selection[KeyWithTranslationsView::translations.name + "." + language.tag + "." + TranslationView::id.name] =
        translationId
      groupByExpressions.add(translationId)

      val translationTextField = translations.get(Translation_.text)
      selection[KeyWithTranslationsView::translations.name + "." + language.tag + "." + TranslationView::text.name] =
        translationTextField
      selection[KeyWithTranslationsView::translations.name + "." + language.tag + "." + TranslationView::state.name] =
        translations.get(Translation_.state)
      fullTextFields.add(translationTextField)
      translationsTextFields.add(translationTextField)
      applyTranslationFilters(language, translationTextField)
    }
  }

  private fun applyTranslationFilters(language: Language, translationTextField: Path<String>) {
    if (params.filterUntranslatedInLang == language.tag) {
      whereConditions.add(translationTextField.isNullOrBlank)
    }
    if (params.filterTranslatedInLang == language.tag) {
      whereConditions.add(translationTextField.isNotNullOrBlank)
    }
  }

  private fun applyGlobalFilters() {
    if (params.filterKeyName != null) {
      whereConditions.add(cb.equal(keyNameExpression, params.filterKeyName))
    } else if (params.filterKeyId != null) {
      whereConditions.add(cb.equal(keyIdExpression, params.filterKeyId))
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
        havingConditions.add(cb.gt(screenshotCountExpression, 0))
      }
      if (params.filterHasNoScreenshot) {
        havingConditions.add(cb.lt(screenshotCountExpression, 1))
      }
      if (!params.search.isNullOrEmpty()) {
        val fullTextRestrictions: MutableSet<Predicate> = HashSet()
        for (fullTextField in fullTextFields) {
          fullTextRestrictions.add(
            cb.like(
              cb.upper(fullTextField),
              "%" + params.search.uppercase(Locale.getDefault()) + "%"
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
      getCursorPredicate()?.let {
        query.where(it)
      }
      val groupBy = listOf(keyIdExpression, *groupByExpressions.toTypedArray())
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
      query.select(cb.count(file))
      return query
    }

  companion object {
    val KEY_NAME_FIELD = KeyWithTranslationsView::keyName.name

    @JvmStatic
    fun getData(
      em: EntityManager,
      project: Project?,
      languages: Set<Language>,
      pageable: Pageable,
      params: GetTranslationsParams = GetTranslationsParams()
    ): Page<KeyWithTranslationsView> {

      var sort = if (pageable.sort.isSorted)
        pageable.sort
      else
        Sort.by(Sort.Order.asc(KEY_NAME_FIELD))

      sort = sort.and(Sort.by(Sort.Direction.ASC, KeyWithTranslationsView::keyId.name))

      var translationsViewBuilder = TranslationsViewBuilder(
        cb = em.criteriaBuilder,
        project = project,
        languages = languages,
        params = params,
        sort = sort,
        cursor = params.cursor?.let { CursorUtil.parseCursor(it) }
      )
      val count = em.createQuery(translationsViewBuilder.countQuery).singleResult
      translationsViewBuilder = TranslationsViewBuilder(
        cb = em.criteriaBuilder,
        project = project,
        languages = languages,
        params = params,
        sort = sort,
        cursor = params.cursor?.let { CursorUtil.parseCursor(it) }
      )
      val query = em.createQuery(translationsViewBuilder.dataQuery).setMaxResults(pageable.pageSize)
      if (params.cursor == null) {
        query.firstResult = pageable.offset.toInt()
      }
      val views = query.resultList.map { KeyWithTranslationsView.of(it) }
      return PageImpl(views, pageable, count)
    }
  }
}
