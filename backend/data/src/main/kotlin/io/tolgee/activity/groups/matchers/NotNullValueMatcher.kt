package io.tolgee.activity.groups.matchers

import io.tolgee.activity.data.PropertyModification
import org.jooq.Condition
import org.jooq.Field
import org.jooq.JSON
import org.jooq.impl.DSL

class NotNullValueMatcher : ActivityGroupValueMatcher {
  override fun match(value: Any?): Boolean {
    if (value is PropertyModification) {
      return value.new != null
    }
    return value != null
  }

  override fun createChildSqlCondition(field: Field<JSON>): Condition {
    return field.isNotNull
  }

  override fun createRootSqlCondition(field: Field<JSON>): Condition {
    val new = DSL.jsonGetAttribute(field, "new")
    return createChildSqlCondition(new)
  }
}
