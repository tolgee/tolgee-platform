package io.tolgee.activity.groups.dataProviders

import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.matchers.modifiedEntity.SqlContext
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.impl.DSL

class CountsProvider(
  private val jooqContext: DSLContext,
  private val groupType: ActivityGroupType,
  private val entityClasses: List<String>,
  private val groupIds: List<Long>,
) {
  private val activityModifiedEntityTable = DSL.table("activity_modified_entity").`as`("ame")
  private val entityClassField = DSL.field("ame.entity_class", String::class.java)
  private val activityRevisionTable = DSL.table("activity_revision").`as`("ar")
  private val activityRevisionActivityGroupsTable = DSL.table("activity_revision_activity_groups").`as`("arag")
  private val activityGroupTable = DSL.table("activity_group").`as`("ag")
  private val groupIdField = DSL.field("arag.activity_groups_id", Long::class.java)
  private val countField = DSL.count()

  fun provide(): Map<Long, Map<String, Int>> {
    val queryResult =
      jooqContext
        .select(
          groupIdField,
          entityClassField,
          countField,
        )
        .from(activityModifiedEntityTable)
        .join(activityRevisionTable)
        .on(
          DSL.field("ame.activity_revision_id", Long::class.java)
            .eq(DSL.field("ar.id", Long::class.java)),
        )
        .join(activityRevisionActivityGroupsTable)
        .on(
          DSL.field("ar.id", Long::class.java)
            .eq(DSL.field("arag.activity_revisions_id", Long::class.java)),
        )
        .join(activityGroupTable).on(
          groupIdField.eq(DSL.field("ag.id", Long::class.java)),
        )
        .where(
          groupIdField.`in`(groupIds)
            .and(entityClassField.`in`(entityClasses))
            .and(groupType.matcher?.match(sqlContext))
            .and(getStringMatcherCondition()),
        )
        .groupBy(entityClassField, groupIdField)
        .fetch()

    return queryResult.groupBy { groupIdField.get(it)!! }.mapValues { rows ->
      rows.value.associate { entityClassField.getValue(it)!! to countField.getValue(it)!! }
    }
  }

  private val sqlContext by lazy {
    SqlContext(
      modificationsField = DSL.field("ame.modifications", JSON::class.java),
      entityClassField = entityClassField,
      revisionTypeField = DSL.field("ame.revision_type", Int::class.java),
      groupIdField = groupIdField,
      baseLanguageField = DSL.field("ar.base_language_id", Long::class.java),
      describingRelationsField = DSL.field("ame.describing_relations", JSON::class.java),
    )
  }

  private fun getStringMatcherCondition(): Condition {
    return groupType.matchingStringProvider?.provide(sqlContext)
      ?.eq(DSL.field("ag.matching_string", String::class.java)) ?: DSL.noCondition()
  }
}
