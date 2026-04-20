package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.dtos.response.CursorValue
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import org.hibernate.query.NullPrecedence
import org.hibernate.query.sqm.tree.select.SqmSortSpecification
import org.springframework.data.domain.Sort

class TranslationsViewQueryBuilder(
  private val cb: CriteriaBuilder,
  private val projectId: Long,
  private val languages: Set<LanguageDto>,
  private val params: TranslationFilters,
  private val sort: Sort,
  private val cursor: Map<String, CursorValue>? = null,
  private val entityManager: EntityManager,
  private val qaEnabled: Boolean,
) {
  private fun <T> getBaseQuery(
    query: CriteriaQuery<T>,
    isCountQuery: Boolean = false,
  ): QueryBase<T> {
    return QueryBase(
      cb = cb,
      projectId = projectId,
      query = query,
      languages = languages,
      params = params,
      entityManager,
      qaEnabled = qaEnabled,
      isCountQuery = isCountQuery,
    )
  }

  private fun getWhereConditions(queryBase: QueryBase<*>): MutableList<Predicate> {
    val where = queryBase.whereConditions.toMutableList()
    val translationConditions = queryBase.translationConditions.toMutableList()
    if (translationConditions.isNotEmpty()) {
      where.add(cb.or(*translationConditions.toTypedArray()))
    }

    val cursorPredicateProvider = CursorPredicateProvider(cb, cursor, queryBase.querySelection, queryBase, languages)
    cursorPredicateProvider()?.let {
      where.add(it)
    }
    return where
  }

  val dataQuery: CriteriaQuery<Array<Any?>>
    get() {
      val query = cb.createQuery(Array<Any?>::class.java)
      val queryBase = getBaseQuery(query)
      val paths = queryBase.querySelection.values.toTypedArray()
      query.multiselect(*paths)
      val orderList = getOrderList(queryBase)

      query.where(*getWhereConditions(queryBase).toTypedArray())
      // No GROUP BY: the main query has no joins that multiply rows. Tag, task, and
      // translation-level filters are all EXISTS subqueries, and the remaining joins
      // (branch, namespace, keyMeta, deletedBy) are all 1:0..1 so they don't duplicate keys.
      query.orderBy(orderList)
      return query
    }

  private fun getOrderList(queryBase: QueryBase<Array<Any?>>): MutableList<Order> {
    val orderList =
      sort
        .asSequence()
        .mapNotNull { order ->
          val expression = resolveSortExpression(queryBase, order.property) ?: return@mapNotNull null
          when (order.direction) {
            Sort.Direction.DESC -> cb.desc(expression)
            else -> cb.asc(expression)
          }
        }.toMutableList()

    if (orderList.isEmpty()) {
      orderList.add(cb.asc(queryBase.keyNameExpression))
    }

    orderList.forEach {
      (it as? SqmSortSpecification)?.let { sortSpec ->
        when {
          sortSpec.isAscending -> sortSpec.nullPrecedence(NullPrecedence.FIRST)
          else -> sortSpec.nullPrecedence(NullPrecedence.LAST)
        }
      }
    }

    return orderList
  }

  /**
   * Resolves a sort property (e.g. `keyName`, `createdAt`, `translations.de.text`) to the
   * actual JPA Criteria expression it refers to. Key-level columns are looked up in the
   * [QueryBase.querySelection] map; translation-text columns are built as scalar correlated
   * subqueries via [QueryBase.scalarTranslationText] because they're no longer in the SELECT list.
   */
  private fun resolveSortExpression(
    queryBase: QueryBase<*>,
    property: String,
  ): Expression<*>? {
    // Fast path: direct column in query selection
    queryBase.querySelection[property]?.let { return it as Expression<*> }

    // Translation text sort: only `.text` is supported because the scalar-subquery approach is
    // designed around a single column per row and `text` is the only field users actually sort
    // by. Other translation fields (id, state) are still allowed for cursor purposes via the
    // `translations.{tag}.text` shape but never reach this branch.
    val (tag, field) = KeyWithTranslationsView.parseTranslationProperty(property) ?: return null
    if (field != TranslationView::text.name) return null
    val language = languages.find { it.tag == tag } ?: return null
    return queryBase.scalarTranslationText(language.id)
  }

  val countQuery: CriteriaQuery<Long>
    get() {
      val query = cb.createQuery(Long::class.java)
      val queryBase = getBaseQuery(query, isCountQuery = true)
      // A plain `count(keyId)` is correct and cheap because the main query never multiplies
      // rows (no join on translations; tag / task / translation filters are all EXISTS
      // subqueries, and the other joins are 1:0..1).
      query.select(cb.count(queryBase.keyIdExpression))
      query.where(*getWhereConditions(queryBase).toTypedArray())
      return query
    }

  val keyIdsQuery: CriteriaQuery<Long>
    get() {
      val query = cb.createQuery(Long::class.java)
      val queryBase = getBaseQuery(query = query, isCountQuery = true)
      query.select(queryBase.keyIdExpression)
      query.where(*getWhereConditions(queryBase).toTypedArray())
      query.distinct(true)
      return query
    }
}
