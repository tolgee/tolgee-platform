package io.tolgee.activity.groups.matchers.modifiedEntity

import io.tolgee.model.translation.Translation
import org.jooq.Field
import org.jooq.impl.DSL

class SetTranslationMatchingStringProvider : MatchingStringProvider {
  override fun provide(context: StoringContext): String? {
    if (context.modifiedEntity.entityClass != Translation::class.simpleName) {
      return null
    }
    return context.modifiedEntity.describingRelations?.get("language")?.entityId?.toString()
      ?: throw IllegalStateException("Language not found")
  }

  override fun provide(context: SqlContext): Field<String> {
    return DSL.field(
      "(${context.describingRelationsField.name} -> 'language' -> 'entityId')::varchar",
      String::class.java,
    )
  }
}
