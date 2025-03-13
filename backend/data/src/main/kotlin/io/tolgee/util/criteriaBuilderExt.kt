package io.tolgee.util

import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root

fun <Y : Comparable<Y>> CriteriaBuilder.greaterThanNullable(
  expression: Expression<out Y>,
  value: Y?,
): Predicate {
  return if (value == null) {
    expression.isNotNull
  } else {
    this.and(expression.isNotNull, this.greaterThan(expression, value))
  }
}

fun <Y : Comparable<Y>> CriteriaBuilder.lessThanNullable(
  expression: Expression<out Y>,
  value: Y?,
): Predicate {
  return if (value == null) {
    this.isTrue(this.literal(false))
  } else {
    this.or(expression.isNull, this.lessThan(expression, value))
  }
}

fun <Y : Comparable<Y>> CriteriaBuilder.greaterThanOrEqualToNullable(
  expression: Expression<out Y>,
  value: Y?,
): Predicate {
  return if (value == null) {
    this.isTrue(this.literal(true))
  } else {
    this.and(expression.isNotNull, this.greaterThanOrEqualTo(expression, value))
  }
}

fun <Y : Comparable<Y>> CriteriaBuilder.lessThanOrEqualToNullable(
  expression: Expression<out Y>,
  value: Y?,
): Predicate {
  return if (value == null) {
    this.isNull(expression)
  } else {
    this.or(expression.isNull, this.lessThanOrEqualTo(expression, value))
  }
}

fun <Y> CriteriaBuilder.equalNullable(
  expression: Expression<out Y>,
  value: Y?,
): Predicate {
  return if (value == null) {
    this.isNull(expression)
  } else {
    this.equal(expression, value)
  }
}

inline fun <reified RootT, reified Result> EntityManager.query(
  fn: CriteriaQuery<Result>.(cb: CriteriaBuilder, root: Root<RootT>) -> Unit,
): TypedQuery<Result> {
  val cb = this.criteriaBuilder
  val cq = cb.createQuery(Result::class.java)
  val root = cq.from(RootT::class.java)
  fn(cq, cb, root)
  return this.createQuery(cq)
}
