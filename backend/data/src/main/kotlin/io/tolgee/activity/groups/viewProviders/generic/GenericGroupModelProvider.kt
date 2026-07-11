package io.tolgee.activity.groups.viewProviders.generic

import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.dataProviders.CountsProvider
import io.tolgee.activity.groups.dataProviders.GroupDataProvider
import org.jooq.DSLContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * Group data for group types without a specialized [io.tolgee.activity.groups.GroupModelProvider]:
 * per-entity-class counts for the group header and paged raw modified entities for the expanded view.
 */
@Component
class GenericGroupModelProvider(
  private val jooqContext: DSLContext,
  private val groupDataProvider: GroupDataProvider,
) {
  fun provideCounts(
    groupType: ActivityGroupType,
    groupIds: List<Long>,
  ): Map<Long, GenericGroupModel> {
    val entityClasses = groupType.matcher?.relevantEntityClasses ?: return emptyMap()
    val counts =
      CountsProvider(
        jooqContext = jooqContext,
        groupType = groupType,
        entityClasses = entityClasses.mapNotNull { it.simpleName },
        groupIds = groupIds,
      ).provide()
    return counts.mapValues { GenericGroupModel(it.value) }
  }

  fun provideItems(
    groupType: ActivityGroupType,
    groupId: Long,
    pageable: Pageable,
  ): Page<GenericGroupItemModel> {
    val entityClass =
      groupType.matcher?.relevantEntityClasses?.firstOrNull()
        ?: return Page.empty(pageable)

    @Suppress("UNCHECKED_CAST")
    val entities =
      groupDataProvider.provideRelevantModifiedEntities(
        groupType = groupType,
        entityClass = entityClass as kotlin.reflect.KClass<out io.tolgee.model.EntityWithId>,
        groupId = groupId,
        pageable = pageable,
      )

    return entities.map {
      GenericGroupItemModel(
        entityClass = it.entityClass,
        entityId = it.entityId,
        description = it.describingData,
        describingRelations = it.describingRelations,
        modifications = it.modifications,
      )
    }
  }
}
