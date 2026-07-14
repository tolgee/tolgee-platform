package io.tolgee.activity.groups.matchers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.activity.data.PropertyModification
import org.jooq.Condition
import org.jooq.Field
import org.jooq.JSON
import org.jooq.impl.DSL

class EqualsValueMatcher(
  val value: Any?,
) : ActivityGroupValueMatcher {
  override fun match(value: Any?): Boolean {
    if (value is PropertyModification) {
      return value.new == this.value
    }
    return this.value == value
  }

  override fun createChildSqlCondition(field: Field<JSON>): Condition {
    // json has no equality operator in postgres, so compare as jsonb;
    // an absent attribute yields SQL NULL, an attribute set to null yields JSON null
    if (value == null) {
      return DSL.condition("({0} is null or {0}::jsonb = 'null'::jsonb)", field)
    }
    return DSL.condition("{0}::jsonb = {1}::jsonb", field, DSL.`val`(jsonValue))
  }

  override fun createRootSqlCondition(field: Field<JSON>): Condition {
    val new = DSL.jsonGetAttribute(field, "new")
    return createChildSqlCondition(new)
  }

  private val jsonValue by lazy {
    JSON.json(jacksonObjectMapper().writeValueAsString(value))
  }
}
