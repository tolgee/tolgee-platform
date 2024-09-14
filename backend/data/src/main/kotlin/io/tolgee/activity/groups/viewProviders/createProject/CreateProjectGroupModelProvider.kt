package io.tolgee.activity.groups.viewProviders.createProject

import io.tolgee.activity.groups.GroupModelProvider
import io.tolgee.activity.groups.baseModels.ActivityGroupLanguageModel
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CreateProjectGroupModelProvider(
  private val jooqContext: DSLContext,
) :
  GroupModelProvider<CreateProjectGroupModel, Nothing> {
  override fun provideGroup(groupIds: List<Long>): Map<Long, CreateProjectGroupModel> {
    val query =
      jooqContext
        .select(
          DSL.field("arag.activity_groups_id", Long::class.java).`as`("groupId"),
          DSL.field("pme.modifications -> 'name' ->> 'new'", String::class.java).`as`("name"),
          DSL.field("pme.modifications -> 'description' ->> 'new'", String::class.java).`as`("description"),
          DSL.field("pme.entity_id").`as`("id"),
          DSL.field("lme.modifications -> 'name' ->> 'new'", String::class.java).`as`("languageName"),
          DSL.field("lme.entity_id", Long::class.java).`as`("languageId"),
          DSL.field("lme.modifications -> 'tag' ->> 'new'", String::class.java).`as`("languageTag"),
          DSL.field("lme.modifications -> 'originalName' ->> 'new'", String::class.java).`as`("languageOriginalName"),
          DSL.field("lme.modifications -> 'flagEmoji' ->> 'new'", String::class.java).`as`("languageFlagEmoji"),
        )
        .from(DSL.table("activity_modified_entity").`as`("pme"))
        .join(DSL.table("activity_revision").`as`("ar"))
        .on(DSL.field("pme.activity_revision_id").eq(DSL.field("ar.id")))
        .join(DSL.table("activity_revision_activity_groups").`as`("arag"))
        .on(DSL.field("ar.id").eq(DSL.field("arag.activity_revisions_id")))
        .join(DSL.table("activity_modified_entity").`as`("lme"))
        .on(
          DSL.field("lme.entity_class").eq(DSL.inline("Language"))
            .and(DSL.field("lme.activity_revision_id").eq(DSL.field("ar.id"))),
        )
        .where(DSL.field("arag.activity_groups_id").`in`(groupIds))
        .and(DSL.field("pme.entity_class").eq(DSL.inline("Project")))
        .fetch()

    return query.groupBy { it["groupId"] as Long }.map { (id, rows) ->
      val languages =
        rows.map {
          ActivityGroupLanguageModel(
            it["languageId"] as Long,
            it["languageName"] as String,
            it["languageOriginalName"] as String,
            it["languageTag"] as String,
            it["languageFlagEmoji"] as String,
          )
        }

      id to
        CreateProjectGroupModel(
          id = id,
          name = rows.first()["name"] as String,
          description = rows.first()["description"] as String?,
          languages = languages,
        )
    }.toMap()
  }

  override fun provideItems(
    groupId: Long,
    pageable: Pageable,
  ): Page<Nothing> {
    return Page.empty()
  }
}
