package io.tolgee.util

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate

abstract class KotlinCriteriaBuilder<T>(
  entityManager: EntityManager,
  result: Class<T>,
) {
  val cb = entityManager.criteriaBuilder
  val query = cb.createQuery(result)

  infix fun Expression<*>.equal(other: Expression<*>): Predicate = cb.equal(this, other)

  infix fun Predicate.or(other: Predicate): Predicate = cb.or(this, other)

  infix fun Predicate.and(other: Predicate): Predicate = cb.and(this, other)
}
