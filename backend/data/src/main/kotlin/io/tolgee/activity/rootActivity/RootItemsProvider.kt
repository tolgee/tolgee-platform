package io.tolgee.activity.rootActivity

import io.tolgee.activity.groups.matchers.modifiedEntity.DefaultMatcher
import io.tolgee.model.EntityWithId
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.table
import org.jooq.impl.DSL.value
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Pageable
import kotlin.reflect.KClass

class RootItemsProvider(
  private val pageable: Pageable,
  private val activityRevisionIds: List<Long>,
  private val rootEntityClass: KClass<out EntityWithId>,
  rootModificationItems: List<DefaultMatcher<*>>?,
  private val applicationContext: ApplicationContext,
) {
  fun provide(): List<ActivityTreeResultItem> {
    val rootItems = getRootItemsRaw()
    return itemsParser.parse(rootItems)
  }

  fun getRootItemsRaw(): List<Array<Any?>> {
    val limit = pageable.pageSize
    val offset = pageable.offset

    val activityModifiedEntity = table("activity_modified_entity")
    val activityDescribingEntity = table("activity_describing_entity")

    val ameEntityClass = field("entity_class", String::class.java)
    val ameDescribingData = field("describing_data", Any::class.java) // Adjust type if needed
    val ameModifications = field("modifications", Any::class.java) // Adjust type if needed
    val ameEntityId = field("entity_id", Long::class.java)
    val ameActivityRevisionId = field("activity_revision_id", Long::class.java)

    val adeEntityClass = field("entity_class", String::class.java)
    val adeData = field("data", Any::class.java) // Adjust type if needed
    val adeEntityId = field("entity_id", Long::class.java)
    val adeActivityRevisionId = field("activity_revision_id", Long::class.java)

    val ameSelect =
      select(
        ameEntityClass,
        ameDescribingData,
        ameModifications,
        ameEntityId.`as`("id"),
        value("AME").`as`("type"),
      ).from(activityModifiedEntity)
        .where(ameEntityClass.eq(rootEntityClass.simpleName))
        .and(ameActivityRevisionId.`in`(activityRevisionIds))

    val adeSubQuery =
      select(ameEntityId)
        .from(activityModifiedEntity)
        .where(ameActivityRevisionId.`in`(activityRevisionIds))
        .and(ameEntityClass.eq(rootEntityClass.simpleName))

    val adeSelect =
      select(
        adeEntityClass,
        adeData,
        value(null as Any?).`as`("modifications"), // Adjust type if needed
        adeEntityId.`as`("id"),
        value("ADE").`as`("type"),
      ).from(activityDescribingEntity)
        .where(adeActivityRevisionId.`in`(activityRevisionIds))
        .and(adeEntityClass.eq(rootEntityClass.simpleName))
        .and(adeEntityId.notIn(adeSubQuery))

    val unionQuery =
      ameSelect.unionAll(adeSelect)
        .orderBy(field("id"))
        .limit(limit)
        .offset(offset)

    return jooqContext.fetch(unionQuery).map { it.intoArray() }.toList()
  }

  private val itemsParser: ActivityItemsParser by lazy {
    applicationContext.getBean(ActivityItemsParser::class.java)
  }

  private val jooqContext: DSLContext by lazy {
    applicationContext.getBean(DSLContext::class.java)
  }
}
