package io.tolgee.activity.groups.matchers.modifiedEntity

import io.tolgee.activity.data.RevisionType
import io.tolgee.model.translation.Translation
import org.jooq.Condition
import org.jooq.impl.DSL

class TranslationMatcher(
  private val type: Type = Type.BASE,
) : ModifiedEntityMatcher {
  private val defaultMatcher by lazy {
    DefaultMatcher(
      entityClass = Translation::class,
      revisionTypes = listOf(RevisionType.ADD, RevisionType.MOD),
      modificationProps = listOf(Translation::text),
    )
  }

  override fun match(context: StoringContext): Boolean {
    if (!defaultMatcher.match(context)) {
      return false
    }

    if (type == Type.ANY) {
      return true
    }

    val entityId =
      context.modifiedEntity.describingRelations?.get("language")?.entityId
        // non-base is the default, so we return true if it is not base translation
        ?: return type == Type.NON_BASE

    val isBase = context.activityRevision.baseLanguageId == entityId

    if (type == Type.BASE) {
      return isBase
    }

    return !isBase
  }

  override fun match(context: SqlContext): Condition {
    val baseCondition = defaultMatcher.match(context)

    if (type == Type.ANY) {
      return baseCondition
    }

    val baseLanguageCondition = context.getSqlCondition()

    if (type == Type.BASE) {
      return baseCondition.and(baseLanguageCondition)
    }

    return baseCondition.and(baseLanguageCondition.not())
  }

  private fun SqlContext.getSqlCondition(): Condition {
    val sqlCondition =
      "(${describingRelationsField.name} -> 'language' -> 'entityId')::bigint = ${baseLanguageField.name}"

    return DSL.condition(sqlCondition)
  }

  enum class Type {
    BASE,
    NON_BASE,
    ANY,
  }
}
