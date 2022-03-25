package io.tolgee.util

import javax.persistence.EntityManager
import javax.persistence.criteria.Expression
import javax.persistence.criteria.Predicate

abstract class KotlinCriteriaBuilder<T>(entityManager: EntityManager, result: Class<T>) {
  val cb = entityManager.criteriaBuilder
  val query = cb.createQuery(result)

  infix fun Expression<*>.equal(other: Expression<*>): Predicate {
    return cb.equal(this, other)
  }

  infix fun Predicate.or(other: Predicate): Predicate {
    return cb.or(this, other)
  }

  infix fun Predicate.and(other: Predicate): Predicate {
    return cb.and(this, other)
  }
}
