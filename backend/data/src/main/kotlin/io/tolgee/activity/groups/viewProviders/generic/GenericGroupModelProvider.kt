package io.tolgee.activity.groups.viewProviders.generic

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.data.ModifiedEntityView
import io.tolgee.activity.groups.dataProviders.CountsProvider
import io.tolgee.activity.groups.dataProviders.GroupDataProvider
import io.tolgee.activity.groups.dataProviders.PagedRelevantModifiedEntitiesProvider
import org.jooq.DSLContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * Group data for group types without a specialized [io.tolgee.activity.groups.GroupModelProvider]:
 * per-entity-class counts for the group header and paged modified entities, with their describing
 * relations resolved, for the expanded view.
 */
@Component
class GenericGroupModelProvider(
  private val jooqContext: DSLContext,
  private val objectMapper: ObjectMapper,
  private val groupDataProvider: GroupDataProvider,
) {
  fun provideCounts(
    groupType: ActivityGroupType,
    groupIds: List<Long>,
  ): Map<Long, GenericGroupModel> {
    val entityClasses = groupType.entityClassNames
    if (entityClasses.isEmpty()) {
      return emptyMap()
    }
    val counts =
      CountsProvider(
        jooqContext = jooqContext,
        groupType = groupType,
        entityClasses = entityClasses,
        groupIds = groupIds,
      ).provide()
    return counts.mapValues { GenericGroupModel(it.value) }
  }

  fun provideItems(
    groupType: ActivityGroupType,
    groupId: Long,
    pageable: Pageable,
  ): Page<GenericGroupItemModel> {
    val entityClasses = groupType.entityClassNames
    if (entityClasses.isEmpty()) {
      return Page.empty(pageable)
    }

    val entities =
      PagedRelevantModifiedEntitiesProvider(
        jooqContext = jooqContext,
        objectMapper = objectMapper,
        groupType = groupType,
        entityClasses = entityClasses,
        groupId = groupId,
        pageable = pageable,
      ).provide()

    val relations = groupDataProvider.getGenericRelations(entities.content)

    return entities.map { entity -> entity.toModel(relations[entity]) }
  }

  private fun ModifiedEntityView.toModel(
    relations: Map<String, io.tolgee.activity.groups.data.DescribingEntityView>?,
  ): GenericGroupItemModel {
    return GenericGroupItemModel(
      entityClass = entityClass,
      entityId = entityId,
      description = describingData,
      relations =
        relations
          ?.mapValues { (_, view) ->
            GenericGroupItemRelationModel(
              entityClass = view.entityClass,
              entityId = view.entityId,
              data = view.data,
            )
          }.orEmpty(),
      modifications = modifications,
    )
  }

  private val ActivityGroupType.entityClassNames: List<String>
    get() = matcher?.relevantEntityClasses?.mapNotNull { it.simpleName }.orEmpty()
}
