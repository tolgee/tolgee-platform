package io.tolgee.activity.groups

import io.tolgee.activity.groups.matchers.ActivityGroupValueMatcher
import io.tolgee.activity.groups.matchers.EqualsValueMatcher
import io.tolgee.api.SimpleUserAccount
import io.tolgee.dtos.queryResults.ActivityGroupView
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.JSON
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.table
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.*

class ActivityGroupsProvider(val projectId: Long, val pageable: Pageable, applicationContext: ApplicationContext) {
  fun get(): PageImpl<ActivityGroupView> {
    page.forEach {
      it.counts = counts[it.id] ?: emptyMap()
      it.data = dataViews[it.id]
    }

    return page
  }

  private val page by lazy {
    val from = table("activity_group").`as`("ag")
    val where = field("ag.project_id").eq(projectId)

    val count = jooqContext.selectCount().from(from).where(where).fetchOne(0, Long::class.java)!!

    val result =
      jooqContext.select(
        field("ag.id"),
        field("ag.type"),
        field("ag.author_id"),
        max(field("ar.timestamp")),
        field("ua.id"),
        field("ua.username"),
        field("ua.name"),
        field("ua.avatar_hash"),
        field("ua.deleted_at").isNotNull,
      )
        .from(from)
        .leftJoin(table("activity_revision_activity_groups").`as`("arag"))
        .on(field("ag.id").eq(field("arag.activity_groups_id")))
        .leftJoin(table("activity_revision").`as`("ar"))
        .on(field("ar.id").eq(field("arag.activity_revisions_id")))
        .leftJoin(table("user_account").`as`("ua")).on(field("ag.author_id").eq(field("ua.id")))
        .where(where)
        .groupBy(field("ag.id"), field("ua.id"))
        .orderBy(max(field("ar.timestamp")).desc())
        .limit(pageable.pageSize)
        .offset(pageable.offset).fetch().map {
          ActivityGroupView(
            it[0] as Long,
            ActivityGroupType.valueOf(it[1] as String),
            it[3] as Date,
            author = object : SimpleUserAccount {
              override val id: Long = it[4] as Long
              override val username: String = it[5] as String
              override val name: String = it[6] as String
              override val avatarHash: String? = it[7] as String?
              override val deleted: Boolean = it[8] as Boolean
            }
          )
        }

    PageImpl(result, pageable, count)
  }

  private val counts by lazy {
    byType.flatMap { (type, items) ->
      getCounts(type, items).map { it.key to it.value }
    }.toMap()
  }

  private val dataViews by lazy {
    byType.flatMap { (type, items) ->
      val provider = type.modelProviderFactory?.invoke(applicationContext)
      provider?.provide(items.map { it.id })?.map { it.key to it.value } ?: emptyList()
    }.toMap()
  }

  private val byType by lazy { page.groupBy { it.type } }

  private fun getModificationCondition(definition: GroupEntityModificationDefinition<*>): Condition {
    return field("ame.entity_class").eq(definition.entityClass.simpleName)
      .and(field("ame.revision_type").`in`(definition.revisionTypes.map { it.ordinal }))
      .also {
        if (definition.modificationProps != null) {
          it.and("array(select jsonb_object_keys(ame.modifications)) && ${allowedModString(definition)}")
        }
      }.also {
        it.and(getAllowedValuesCondition(definition))
      }
      .also {
        it.and(getDeniedValuesCondition(definition))
      }
  }

  private fun getAllowedValuesCondition(definition: GroupEntityModificationDefinition<*>): Condition {
    val allowedValues = definition.allowedValues ?: return DSL.noCondition()
    val conditions = getValueMatcherConditions(allowedValues)
    return DSL.and(conditions)
  }

  private fun getDeniedValuesCondition(definition: GroupEntityModificationDefinition<*>): Condition {
    val allowedValues = definition.allowedValues ?: return DSL.noCondition()
    val conditions = getValueMatcherConditions(allowedValues)
    return DSL.not(DSL.or(conditions))
  }

  private fun getValueMatcherConditions(values: Map<out Any, Any?>): List<Condition> {
    return values.map {
      val matcher =
        when (val requiredValue = it.value) {
          is ActivityGroupValueMatcher -> requiredValue
          else -> EqualsValueMatcher(requiredValue)
        }

      @Suppress("UNCHECKED_CAST")
      matcher.createRootSqlCondition(field("ame.modifications") as Field<JSON>)
    }
  }

  private fun allowedModString(definition: GroupEntityModificationDefinition<*>): String {
    return "{${definition.modificationProps?.joinToString(",")}}"
  }

  private fun getCounts(
    type: ActivityGroupType,
    items: List<ActivityGroupView>,
  ): MutableMap<Long, MutableMap<String, Long>> {
    val groupIds = items.map { it.id }
    val result = mutableMapOf<Long, MutableMap<String, Long>>()
    type.modifications
      .filter { it.countInView }
      .forEach { definition ->
        val queryResult =
          jooqContext
            .select(field("arag.activity_groups_id"), DSL.count())
            .from(table("activity_modified_entity").`as`("ame"))
            .join(table("activity_revision").`as`("ar"))
            .on(field("ar.id").eq(field("ame.activity_revision_id")))
            .join(table("activity_revision_activity_groups").`as`("arag"))
            .on(field("ar.id").eq(field("arag.activity_revisions_id")))
            .where(field("arag.activity_groups_id").`in`(groupIds).and(getModificationCondition(definition)))
            .groupBy(field("arag.activity_groups_id")).fetch()
        queryResult.forEach {
          val groupMap =
            result.computeIfAbsent(it[0] as Long) {
              mutableMapOf()
            }
          groupMap[definition.entityClass.simpleName!!] = (it[1] as Int).toLong()
        }
      }
    return result
  }

  private val jooqContext = applicationContext.getBean(DSLContext::class.java)
}
