package io.tolgee.activity.groups.dataProviders

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.data.ModifiedEntityView
import io.tolgee.activity.groups.matchers.modifiedEntity.SqlContext
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.SelectField
import org.jooq.impl.DSL
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class RelevantModifiedEntitiesProvider(
  private val jooqContext: DSLContext,
  private val objectMapper: ObjectMapper,
  private val groupType: ActivityGroupType,
  private val entityClasses: List<String>,
  private val groupId: Long,
  private val pageable: Pageable,
) {
  private val activityModifiedEntityTable = DSL.table("activity_modified_entity").`as`("ame")
  private val entityClassField = DSL.field("ame.entity_class", String::class.java)
  private val activityRevisionTable = DSL.table("activity_revision").`as`("ar")
  private val activityRevisionActivityGroupsTable = DSL.table("activity_revision_activity_groups").`as`("arag")
  private val groupIdField = DSL.field("arag.activity_groups_id", Long::class.java)
  private val entityIdField = DSL.field("ame.entity_id", Long::class.java)
  private val describingDataField = DSL.field("ame.describing_data", JSON::class.java)
  private val describingRelationsField = DSL.field("ame.describing_relations", JSON::class.java)
  private val modificationsField = DSL.field("ame.modifications", JSON::class.java)
  private val sqlContext =
    SqlContext(
      modificationsField = DSL.field("ame.modifications", JSON::class.java),
      entityClassField = entityClassField,
      revisionTypeField = DSL.field("ame.revision_type", Int::class.java),
    )

  fun provide(): Page<ModifiedEntityView> {
    val queryResult =
      getDataQuery()
        .fetch()

    val content =
      queryResult.map {
        ModifiedEntityView(
          entityId = it.get(entityIdField)!!,
          entityClass = it.get(entityClassField)!!,
          describingData = objectMapper.readValue(it.get(describingDataField).data()),
          describingRelations = objectMapper.readValue(it.get(describingRelationsField).data()),
          modifications = objectMapper.readValue(it.get(modificationsField).data()),
        )
      }

    val count = getCountQuery().fetchOne(0, Int::class.java) ?: 0

    return PageImpl(content, pageable, count.toLong())
  }

  private fun getDataQuery() =
    getQueryBase(
      entityIdField,
      entityClassField,
      describingDataField,
      describingRelationsField,
      modificationsField,
    )

  private fun getCountQuery() = getQueryBase(DSL.count())

  private fun getQueryBase(vararg fields: SelectField<*>) =
    jooqContext
      .select(*fields)
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
      .where(
        groupIdField.`in`(groupId).and(entityClassField.`in`(entityClasses)).and(
          groupType.matcher?.match(sqlContext),
        ),
      )
}
