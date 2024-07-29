package io.tolgee.activity.groups.matchers

import org.jooq.Condition
import org.jooq.Field
import org.jooq.JSON

interface ActivityGroupValueMatcher {
  fun match(value: Any?): Boolean

  fun createChildSqlCondition(field: Field<JSON>): Condition

  fun createRootSqlCondition(field: Field<JSON>): Condition

  companion object {
    fun notNull() = NotNullValueMatcher()

    fun modification(pair: Pair<ActivityGroupValueMatcher, ActivityGroupValueMatcher>) =
      ModificationValueMatcher(pair.first, pair.second)

    fun eq(value: Any?) = EqualsValueMatcher(value)
  }
}
