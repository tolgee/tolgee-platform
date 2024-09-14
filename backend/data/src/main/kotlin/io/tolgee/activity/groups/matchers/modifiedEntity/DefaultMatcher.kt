package io.tolgee.activity.groups.matchers.modifiedEntity

import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.data.RevisionType
import io.tolgee.activity.groups.matchers.ActivityGroupValueMatcher
import io.tolgee.activity.groups.matchers.EqualsValueMatcher
import org.jooq.Condition
import org.jooq.impl.DSL
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class DefaultMatcher<T : Any>(
  val entityClass: KClass<T>,
  val revisionTypes: List<RevisionType>,
  val modificationProps: List<KProperty1<T, *>>? = null,
  val allowedValues: Map<KProperty1<T, *>, Any?>? = null,
  val deniedValues: Map<KProperty1<T, *>, Any?>? = null,
) : ModifiedEntityMatcher {
  override fun match(context: StoringContext): Boolean {
    if (context.modifiedEntity.entityClass != entityClass.simpleName) {
      return false
    }

    if (context.modifiedEntity.revisionType !in revisionTypes) {
      return false
    }

    val hasModifiedColumn =
      context.modifiedEntity.modifications.any { modification ->
        modificationProps?.any { it.name == modification.key } ?: true
      }

    if (!hasModifiedColumn) {
      return false
    }

    if (!isAllValuesAllowed(context)) {
      return false
    }

    return isNoValueDenied(context)
  }

  private fun isAllValuesAllowed(context: StoringContext): Boolean {
    return context.modifiedEntity.modifications.all { modification ->
      val allowedValueDefinition =
        allowedValues?.filterKeys { it.name == modification.key }?.values?.firstOrNull() ?: return@all true
      compareValue(allowedValueDefinition, modification.value)
    }
  }

  private fun isNoValueDenied(context: StoringContext): Boolean {
    val isAnyDenied =
      context.modifiedEntity.modifications.any { modification ->
        deniedValues?.any { it.key.name == modification.key && compareValue(it.value, modification.value) } ?: false
      }
    return !isAnyDenied
  }

  private fun compareValue(
    matcher: Any?,
    modification: PropertyModification,
  ): Boolean {
    return when (matcher) {
      is ActivityGroupValueMatcher -> matcher.match(modification)
      else -> matcher == modification.new
    }
  }

  override fun match(context: SqlContext): Condition {
    return DSL.and(
      getEntityClassCondition(context),
      getRevisionTypeCondition(context),
      getModificationPropsCondition(context),
      getAllowedValuesCondition(context),
      getDeniedValuesCondition(context),
    )
  }

  private fun getModificationPropsCondition(context: SqlContext): Condition {
    if (modificationProps != null) {
      val props = modificationProps.map { it.name }.toTypedArray()
      return DSL.arrayOverlap(
        DSL.field(
          "array(select jsonb_object_keys(${context.modificationsField.name}))::varchar[]",
          Array<String>::class.java,
        ),
        props,
      )
    }
    return DSL.noCondition()
  }

  private fun getRevisionTypeCondition(context: SqlContext): Condition {
    return context.revisionTypeField.`in`(revisionTypes.map { it.ordinal })
  }

  private fun getEntityClassCondition(context: SqlContext): Condition {
    return context.entityClassField.eq(entityClass.simpleName)
  }

  private fun getAllowedModString(): String {
    return "{${modificationProps?.joinToString(",") { "'${it.name}'" }}}"
  }

  private fun getAllowedValuesCondition(context: SqlContext): Condition {
    val allowedValues = allowedValues ?: return DSL.noCondition()
    val conditions = getValueMatcherConditions(context, allowedValues)
    return DSL.and(conditions)
  }

  private fun getDeniedValuesCondition(context: SqlContext): Condition {
    val deniedValues = deniedValues ?: return DSL.noCondition()
    val conditions = getValueMatcherConditions(context, deniedValues)
    return DSL.not(DSL.or(conditions))
  }

  private fun getValueMatcherConditions(
    context: SqlContext,
    values: Map<out Any, Any?>,
  ): List<Condition> {
    return values.map {
      val matcher =
        when (val requiredValue = it.value) {
          is ActivityGroupValueMatcher -> requiredValue
          else -> EqualsValueMatcher(requiredValue)
        }
      matcher.createRootSqlCondition(context.modificationsField)
    }
  }
}
