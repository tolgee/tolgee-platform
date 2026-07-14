package io.tolgee.activity.groups.matchers.modifiedEntity

import org.jooq.Condition
import org.jooq.impl.DSL
import kotlin.reflect.KClass

interface ModifiedEntityMatcher {
  val relevantEntityClasses: Set<KClass<*>>

  fun match(context: StoringContext): Boolean

  fun match(context: SqlContext): Condition

  fun and(other: ModifiedEntityMatcher): ModifiedEntityMatcher {
    return object : ModifiedEntityMatcher {
      override val relevantEntityClasses =
        this@ModifiedEntityMatcher.relevantEntityClasses + other.relevantEntityClasses

      override fun match(context: StoringContext): Boolean {
        return this@ModifiedEntityMatcher.match(context) && other.match(context)
      }

      override fun match(context: SqlContext): Condition {
        return DSL.and(this@ModifiedEntityMatcher.match(context), other.match(context))
      }
    }
  }

  fun or(other: ModifiedEntityMatcher): ModifiedEntityMatcher {
    return object : ModifiedEntityMatcher {
      override val relevantEntityClasses =
        this@ModifiedEntityMatcher.relevantEntityClasses + other.relevantEntityClasses

      override fun match(context: StoringContext): Boolean {
        return this@ModifiedEntityMatcher.match(context) || other.match(context)
      }

      override fun match(context: SqlContext): Condition {
        return DSL.or(this@ModifiedEntityMatcher.match(context), other.match(context))
      }
    }
  }
}
