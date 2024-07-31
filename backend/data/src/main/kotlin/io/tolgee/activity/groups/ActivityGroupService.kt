package io.tolgee.activity.groups

import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.queryResults.ActivityGroupView
import io.tolgee.model.activity.ActivityGroup
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.repository.activity.ActivityGroupRepository
import org.jooq.DSLContext
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ActivityGroupService(
  private val applicationContext: ApplicationContext,
  private val activityGroupRepository: ActivityGroupRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val jooqContext: DSLContext,
) {
  fun addToGroup(
    activityRevision: ActivityRevision,
    modifiedEntities: ModifiedEntitiesType,
  ) {
    ActivityGrouper(activityRevision, modifiedEntities, applicationContext).addToGroup()
  }

  fun getOrCreateCurrentActivityGroupDto(
    type: ActivityGroupType,
    projectId: Long?,
    authorId: Long?,
  ): ActivityGroupDto {
    val existing = findSuitableExistingSuitableGroupDto(type, projectId, authorId)
    return existing ?: createActivityGroupDto(type, projectId, authorId)
  }

  private fun createActivityGroupDto(
    type: ActivityGroupType,
    projectId: Long?,
    authorId: Long?,
  ): ActivityGroupDto {
    val entity = createActivityGroup(type, projectId, authorId)
    return ActivityGroupDto(
      entity.id,
      entity.type,
      currentDateProvider.date,
      currentDateProvider.date,
    )
  }

  private fun createActivityGroup(
    type: ActivityGroupType,
    projectId: Long?,
    authorId: Long?,
  ): ActivityGroup {
    return ActivityGroup(
      type = type,
    ).also {
      it.authorId = authorId
      it.projectId = projectId
      activityGroupRepository.saveAndFlush(it)
    }
  }

  private fun findSuitableExistingSuitableGroupDto(
    type: ActivityGroupType,
    projectId: Long?,
    authorId: Long?,
  ): ActivityGroupDto? {
    val latest = findLatest(type, authorId, projectId) ?: return null
    if (latest.isTooOld || latest.lastActivityTooEarly) {
      return null
    }
    return latest
  }

  private fun findLatest(
    type: ActivityGroupType,
    authorId: Long?,
    projectId: Long?,
  ): ActivityGroupDto? {
    val result =
      activityGroupRepository.findLatest(
        groupTypeName = type.name,
        authorId = authorId,
        projectId = projectId,
      )

    if (result.isEmpty()) {
      return null
    }

    val single = result.single()

    return ActivityGroupDto(
      single[0] as Long,
      ActivityGroupType.valueOf(single[1] as String),
      // if the group is empty we can just consider it as created now
      single[2] as Date? ?: currentDateProvider.date,
      single[3] as Date? ?: currentDateProvider.date,
    )
  }

  @Transactional
  fun getProjectActivityGroups(
    projectId: Long,
    pageable: Pageable,
  ): PageImpl<ActivityGroupView> {
    return ActivityGroupsProvider(projectId, pageable, applicationContext).get()
  }

  private val ActivityGroupDto.isTooOld: Boolean
    get() {
      return this.earliestTimestamp.time + GROUP_MAX_AGE < currentDateProvider.date.time
    }

  private val ActivityGroupDto.lastActivityTooEarly: Boolean
    get() {
      return latestTimestamp.time + GROUP_MAX_LAST_ACTIVITY_AGE < currentDateProvider.date.time
    }

  companion object {
    const val GROUP_MAX_AGE = 1000 * 60 * 60 * 24
    const val GROUP_MAX_LAST_ACTIVITY_AGE = 1000 * 60 * 60 * 2
  }
}
