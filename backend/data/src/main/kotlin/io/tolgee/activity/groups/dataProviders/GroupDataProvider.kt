package io.tolgee.activity.groups.dataProviders

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.data.ModifiedEntityView
import io.tolgee.model.EntityWithId
import org.jooq.DSLContext
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

    return RelevantModifiedEntitiesProvider(
      jooqContext = jooqContext,
      objectMapper = objectMapper,
      groupType = groupType,
      entityClasses = listOf(simpleNameString),
      groupId = groupId,
      pageable = pageable,
    ).provide()
  }
}
