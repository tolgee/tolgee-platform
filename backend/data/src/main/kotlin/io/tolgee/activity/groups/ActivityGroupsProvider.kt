package io.tolgee.activity.groups

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.api.SimpleUserAccount
import io.tolgee.dtos.queryResults.ActivityGroupView
import io.tolgee.dtos.request.ActivityGroupFilters
import org.jooq.CaseWhenStep
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.jsonArrayAgg
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.table
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.*

class ActivityGroupsProvider(
  val projectId: Long,
  val pageable: Pageable,
  val filters: ActivityGroupFilters,
  applicationContext: ApplicationContext,
) {
  fun get(): PageImpl<ActivityGroupView> {
    page.forEach {
      it.data = dataViews[it.id]
    }

    return page
  }

  private val page by lazy {
    val from = table("activity_group").`as`("ag")
    var where = field("ag.project_id").eq(projectId)

    var having = DSL.noCondition()

    val lmeJsonArrayAggField = jsonArrayAgg(field("lme.entity_id")).`as`("lme_entity_ids")
    val ldeJsonArrayAggField = jsonArrayAgg(field("lde.entity_id")).`as`("lde_entity_ids")

    if (filters.filterType != null) {
      where = where.and(field("ag.type").eq(filters.filterType!!.name))
    }

    if (filters.filterLanguageIdIn != null) {
      val languageIdsJson = JSON.json(objectMapper.writeValueAsString(filters.filterLanguageIdIn))
      having = lmeJsonArrayAggField.contains(languageIdsJson).and(ldeJsonArrayAggField.contains(languageIdsJson))
    }

    if (filters.filterAuthorUserIdIn != null) {
      where = where.and(field("ag.author_id").`in`(filters.filterAuthorUserIdIn))
    }

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
        lmeJsonArrayAggField,
        ldeJsonArrayAggField,
      )
        .from(from)
        .leftJoin(table("activity_revision_activity_groups").`as`("arag"))
        .on(field("ag.id").eq(field("arag.activity_groups_id")))
        .leftJoin(table("activity_revision").`as`("ar"))
        .on(field("ar.id").eq(field("arag.activity_revisions_id")))
        .leftJoin(table("user_account").`as`("ua")).on(field("ag.author_id").eq(field("ua.id")))
        .leftJoin(table("activity_modified_entity").`as`("lme")).on(
          field("ar.id").eq(field("lme.activity_revision_id"))
            .and(field("lme.entity_class").eq("Language")),
        ).leftJoin(table("activity_describing_entity").`as`("lde")).on(
          field("ar.id").eq(field("lde.activity_revision_id"))
            .and(field("lde.entity_class").eq("Language")),
        )
        .where(where)
        .groupBy(field("ag.id"), field("ua.id"))
        .having(having)
        .orderBy(max(field("ar.timestamp")).desc(), orderedTypesField?.desc())
        .limit(pageable.pageSize)
        .offset(pageable.offset).fetch().map {
          ActivityGroupView(
            it[0] as Long,
            ActivityGroupType.valueOf(it[1] as String),
            it[3] as Date,
            author =
              object : SimpleUserAccount {
                override val id: Long = it[4] as Long
                override val username: String = it[5] as String
                override val name: String = it[6] as String
                override val avatarHash: String? = it[7] as String?
                override val deleted: Boolean = it[8] as Boolean
              },
            mentionedLanguageIds = parseMentionedLanguageIds(it),
          )
        }

    PageImpl(result, pageable, count)
  }

  private fun parseMentionedLanguageIds(it: org.jooq.Record): List<Long> {
    val lmeIds = it.getJsonValue<List<Long?>>("lme_entity_ids") ?: emptyList()
    val ldeIds = it.getJsonValue<List<Long?>>("lde_entity_ids") ?: emptyList()
    return (lmeIds + ldeIds).filterNotNull().toSet().toList()
  }

  private inline fun <reified T : Any> org.jooq.Record.getJsonValue(fieldName: String): T? {
    val string = this.getValue(fieldName, String::class.java) ?: return null
    return objectMapper.readValue(string)
  }

  private val dataViews by lazy {
    byType.flatMap { (type, items) ->
      val provider =
        type.modelProviderFactoryClass?.let { applicationContext.getBean(it.java) }
      provider?.provideGroup(items.map { it.id })?.map { it.key to it.value } ?: emptyList()
    }.toMap()
  }

  private val byType by lazy { page.groupBy { it.type } }

  private val jooqContext = applicationContext.getBean(DSLContext::class.java)

  private val objectMapper = applicationContext.getBean(ObjectMapper::class.java)

  private val orderedTypesField by lazy {
    val choose = DSL.choose(field("ag.type"))
    var whenStep: CaseWhenStep<Any, Int>? = null
    ActivityGroupType.getOrderedTypes().mapIndexed { index, type ->
      whenStep?.let {
        whenStep = it.`when`(type.name, index)
      } ?: let {
        whenStep = choose.`when`(type.name, index)
      }
    }
    whenStep?.otherwise(0)
  }
}
