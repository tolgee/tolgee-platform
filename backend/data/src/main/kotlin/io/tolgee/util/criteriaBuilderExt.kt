package io.tolgee.util

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Expression
import javax.persistence.criteria.Predicate

@Suppress("TYPE_MISMATCH_WARNING")
fun CriteriaBuilder.greaterThanNullable(
  expression: Expression<String>,
  value: String?
): Predicate {
  if (value == null) {
    return expression.isNotNull
  }
  return this.and(expression.isNotNull, this.greaterThan(expression, value as String?))
}

@Suppress("TYPE_MISMATCH_WARNING")
fun CriteriaBuilder.lessThanNullable(
  expression: Expression<String>,
  value: String?
): Predicate {
  if (value == null) {
    return this.isTrue(this.literal(false))
  }
  return this.or(expression.isNull, this.lessThan(expression, value as String?))
}

@Suppress("TYPE_MISMATCH_WARNING")
fun CriteriaBuilder.greaterThanOrEqualToNullable(
  expression: Expression<String>,
  value: String?
): Predicate {
  if (value == null) {
    return this.isTrue(this.literal(true))
  }
  return this.and(expression.isNotNull, this.greaterThanOrEqualTo(expression, value as String?))
}

@Suppress("TYPE_MISMATCH_WARNING")
fun CriteriaBuilder.lessThanOrEqualToNullable(
  expression: Expression<String>,
  value: String?
): Predicate {
  if (value == null) {
    return this.isNull(expression)
  }
  return this.or(expression.isNull, this.lessThanOrEqualTo(expression, value as String?))
}

@Suppress("TYPE_MISMATCH_WARNING")
fun CriteriaBuilder.equalNullable(
  expression: Expression<String>,
  value: Any?
): Predicate {
  if (value == null) {
    return this.isNull(expression)
  }
  return this.equal(expression, value)
}
