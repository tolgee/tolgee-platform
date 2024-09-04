package io.tolgee.activity.groups.dataProviders

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.data.ModifiedEntityView
import io.tolgee.model.EntityWithId
import io.tolgee.model.StandardAuditModel
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class GroupDataProvider(
  private val jooqContext: DSLContext,
  private val objectMapper: ObjectMapper,
) {
  fun provideCounts(
    groupType: ActivityGroupType,
    entityClass: KClass<out EntityWithId>,
    groupIds: List<Long>,
  ): Map<Long, Int> {
    val simpleNameString = entityClass.java.simpleName

    val result =
      CountsProvider(
        groupType = groupType,
        entityClasses = listOf(simpleNameString),
        groupIds = groupIds,
        jooqContext = jooqContext,
      ).provide()

    return result.map { (groupId, counts) ->
      groupId to (counts[simpleNameString] ?: 0)
    }.toMap()
  }

  fun provideRelevantModifiedEntities(
    groupType: ActivityGroupType,
    entityClass: KClass<out EntityWithId>,
    groupId: Long,
    pageable: Pageable,
  ): Page<ModifiedEntityView> {
    val simpleNameString = entityClass.java.simpleName

    return PagedRelevantModifiedEntitiesProvider(
      jooqContext = jooqContext,
      objectMapper = objectMapper,
      groupType = groupType,
      entityClasses = listOf(simpleNameString),
      groupId = groupId,
      pageable = pageable,
    ).provide()
  }

  fun getRelatedEntities(
    entities: Page<ModifiedEntityView>,
    groupType: ActivityGroupType,
    descriptionMapping: List<DescriptionMapping>,
    groupId: Long,
  ): List<ModifiedEntityView> {
    val entityClasses = descriptionMapping.map { it.entityClass }.toSet().map { it.java.simpleName }

    return RelevantModifiedEntitiesProvider(
      jooqContext = jooqContext,
      objectMapper = objectMapper,
      groupType = groupType,
      entityClasses = entityClasses,
      additionalFilter = { context ->
        DSL
          .and(
            context.groupIdField.eq(groupId),
          )
          .and(
            DSL.or(
              descriptionMapping.flatMap { mapping ->
                mapping.entityIds.map {
                  DSL.condition("(ame.describing_relations -> ? -> 'id')::bigint = ?", mapping.field, it)
                    .and(context.entityClassField.eq(mapping.entityClass.simpleName))
                }
              },
            ),
          )
      },
    ).provide()
  }
}

data class DescriptionMapping(
  val entityClass: KClass<out StandardAuditModel>,
  val field: String,
  val entityIds: List<Long>,
)
