package io.tolgee.activity.groups.dataProviders

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.data.DescribingEntityView
import io.tolgee.activity.groups.data.DescribingMapping
import io.tolgee.activity.groups.data.ModifiedEntityView
import io.tolgee.activity.groups.data.RelatedMapping
import io.tolgee.model.EntityWithId
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
    groupType: ActivityGroupType,
    relatedMappings: List<RelatedMapping>,
    groupId: Long,
  ): Map<ModifiedEntityView, Map<RelatedMapping, List<ModifiedEntityView>>> {
    val entityClasses = relatedMappings.map { it.entityClass }.toSet().map { it.java.simpleName }

    val entities =
      RelevantModifiedEntitiesProvider(
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
                relatedMappings.flatMap { mapping ->
                  mapping.entities.map {
                    DSL.condition(
                      "(${context.describingRelationsField.name} -> ? -> 'entityId')::bigint = ?",
                      mapping.field,
                      it.entityId,
                    )
                      .and(context.entityClassField.eq(mapping.entityClass.simpleName))
                  }
                },
              ),
            )
        },
      ).provide()

    val allParents = relatedMappings.flatMap { it.entities }

    return allParents.associateWith { parent ->
      relatedMappings.associateWith { mapping ->
        entities.filter { entity ->
          entity.entityClass == mapping.entityClass.simpleName &&
            entity.describingRelations.any { mapping.field == it.key && it.value?.entityId == parent.entityId }
        }
      }
    }
  }

  fun getDescribingEntities(
    entities: Iterable<ModifiedEntityView>,
    describingMapping: List<DescribingMapping>,
  ): Map<ModifiedEntityView, List<DescribingEntityView>> {
    return DescribingEntitiesProvider(
      entities = entities,
      jooqContext = jooqContext,
      objectMapper = objectMapper,
      describingMapping = describingMapping,
    ).provide()
  }
}
