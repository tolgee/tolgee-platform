package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.dtos.response.CursorValue
import io.tolgee.security.authentication.AuthenticationFacade
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
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
  private val authenticationFacade: AuthenticationFacade,
) {
  private fun <T> getBaseQuery(query: CriteriaQuery<T>): QueryBase<T> {
    return QueryBase(
      cb = cb,
      projectId = projectId,
      query = query,
      languages = languages,
      params = params,
      entityManager,
      authenticationFacade,
    )
  }

  private fun getWhereConditions(queryBase: QueryBase<*>): MutableList<Predicate> {
    val where = queryBase.whereConditions.toMutableList()
    val translationConditions = queryBase.translationConditions.toMutableList()
    if (translationConditions.isNotEmpty()) {
      where.add(cb.or(*translationConditions.toTypedArray()))
    }

    val cursorPredicateProvider = CursorPredicateProvider(cb, cursor, queryBase.querySelection)
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

      val groupBy = listOf(queryBase.keyIdExpression, *queryBase.groupByExpressions.toTypedArray())
      query.where(*getWhereConditions(queryBase).toTypedArray())

      query.groupBy(groupBy)
      query.orderBy(orderList)
      return query
    }

  private fun getOrderList(queryBase: QueryBase<Array<Any?>>): MutableList<Order> {
    val orderList =
      sort
        .asSequence()
        .filter { queryBase.querySelection[it.property] != null }
        .map {
          val expression = queryBase.querySelection[it.property] as Expression<*>
          when (it.direction) {
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

  val countQuery: CriteriaQuery<Long>
    get() {
      val query = cb.createQuery(Long::class.java)
      val queryBase = getBaseQuery(query)
      val file = query.roots.iterator().next() as Root<*>
      query.select(cb.countDistinct(file))
      query.where(*getWhereConditions(queryBase).toTypedArray())
      return query
    }

  val keyIdsQuery: CriteriaQuery<Long>
    get() {
      val query = cb.createQuery(Long::class.java)
      val queryBase = getBaseQuery(query = query)
      query.select(queryBase.keyIdExpression)
      query.where(*getWhereConditions(queryBase).toTypedArray())
      query.distinct(true)
      return query
    }
}
