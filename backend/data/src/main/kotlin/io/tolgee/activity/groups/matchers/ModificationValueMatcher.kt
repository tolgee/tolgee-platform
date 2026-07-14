package io.tolgee.activity.groups.matchers

import io.tolgee.activity.data.PropertyModification
import org.jooq.Condition
import org.jooq.Field
import org.jooq.JSON
import org.jooq.impl.DSL

class ModificationValueMatcher(
  val old: ActivityGroupValueMatcher,
  val new: ActivityGroupValueMatcher,
) : ActivityGroupValueMatcher {
  override fun match(value: Any?): Boolean {
    if (value is PropertyModification) {
      return old.match(value.old) && new.match(value.new)
    }
    throw IllegalArgumentException("Value is not PropertyModification")
  }

  override fun createChildSqlCondition(field: Field<JSON>): Condition {
    throw UnsupportedOperationException("ModificationValueMatcher cannot be used as child condition")
  }

  override fun createRootSqlCondition(field: Field<JSON>): Condition {
    val oldField = DSL.jsonGetAttribute(field, "old")
    val newField = DSL.jsonGetAttribute(field, "new")
    return old.createChildSqlCondition(oldField).and(new.createChildSqlCondition(newField))
  }
}
