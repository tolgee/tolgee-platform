package io.tolgee.activity.groups

import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.queryResults.ActivityGroupView
import io.tolgee.dtos.request.ActivityGroupFilters
import io.tolgee.model.activity.ActivityGroup
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.repository.activity.ActivityGroupRepository
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
) {
  fun addToGroup(
    activityRevision: ActivityRevision,
    modifiedEntities: ModifiedEntitiesType,
  ) {
    ActivityGrouper(activityRevision, modifiedEntities, applicationContext).addToGroup()
  }

  fun getOrCreateCurrentActivityGroupDto(
    type: ActivityGroupType,
    matchingStrings: Set<String?>,
    projectId: Long?,
    authorId: Long?,
  ): Map<String?, ActivityGroupDto> {
    return matchingStrings.associateWith { matchingString ->
      val existing = findSuitableExistingSuitableGroupDto(type, matchingString, projectId, authorId)
      val group = existing ?: createActivityGroupDto(type, matchingString, projectId, authorId)
      group
    }
  }

  private fun createActivityGroupDto(
    type: ActivityGroupType,
    matchingString: String?,
    projectId: Long?,
    authorId: Long?,
  ): ActivityGroupDto {
    val entity = createActivityGroup(type, matchingString, projectId, authorId)
    return ActivityGroupDto(
      entity.id,
      entity.type,
      currentDateProvider.date,
      currentDateProvider.date,
      matchingString = entity.matchingString,
    )
  }

  private fun createActivityGroup(
    type: ActivityGroupType,
    matchingString: String?,
    projectId: Long?,
    authorId: Long?,
  ): ActivityGroup {
    return ActivityGroup(
      type = type,
    ).also {
      it.authorId = authorId
      it.projectId = projectId
      activityGroupRepository.saveAndFlush(it)
      it.matchingString = matchingString
    }
  }

  private fun findSuitableExistingSuitableGroupDto(
    type: ActivityGroupType,
    matchingString: String?,
    projectId: Long?,
    authorId: Long?,
  ): ActivityGroupDto? {
    val latest = findLatest(type, matchingString, authorId, projectId) ?: return null
    if (latest.isTooOld || latest.lastActivityTooEarly) {
      return null
    }
    return latest
  }

  private fun findLatest(
    type: ActivityGroupType,
    matchingString: String?,
    authorId: Long?,
    projectId: Long?,
  ): ActivityGroupDto? {
    val result =
      activityGroupRepository.findLatest(
        groupTypeName = type.name,
        matchingString = matchingString,
        authorId = authorId,
        projectId = projectId,
      )

    if (result.isEmpty()) {
      return null
    }

    val single = result.single()

    return single.mapToGroupDto()
  }

  private fun Array<Any?>.mapToGroupDto(): ActivityGroupDto {
    return ActivityGroupDto(
      this[0] as Long,
      ActivityGroupType.valueOf(this[1] as String),
      // if the group is empty we can just consider it as created now
      this[3] as Date? ?: currentDateProvider.date,
      this[4] as Date? ?: currentDateProvider.date,
      matchingString = this[2] as String?,
    )
  }

  @Transactional
  fun getProjectActivityGroups(
    projectId: Long,
    pageable: Pageable,
    activityGroupFilters: ActivityGroupFilters,
  ): PageImpl<ActivityGroupView> {
    return ActivityGroupsProvider(projectId, pageable, activityGroupFilters, applicationContext).get()
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
