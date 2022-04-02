package io.tolgee.activity

import io.tolgee.activity.activities.common.Activity
import io.tolgee.activity.activities.common.ActivityProvider
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.ActivityRevisionRepository
import io.tolgee.service.UserAccountService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class ActivityService(
  private val entityManager: EntityManager,
  private val activityRevisionRepository: ActivityRevisionRepository,
  private val activityProvider: ActivityProvider,
  private val userAccountService: UserAccountService
) {
  fun onActivity(activityModifiedEntity: ActivityModifiedEntity) {
    entityManager.persist(activityModifiedEntity.activityRevision)
    entityManager.persist(activityModifiedEntity)
  }

  fun getModifiedEntitiesForEachRevision(revisionIds: Collection<Long>): Map<Long, List<ActivityModifiedEntity>> {
    return activityRevisionRepository
      .getModificationsForEachRevision(revisionIds).groupBy { it.activityRevision.id }
  }

  fun getProjectActivity(projectId: Long, pageable: Pageable): Page<ProjectActivityView> {
    val revisions = getProjectActivityRevisions(projectId, pageable)

    val authors = userAccountService.getAllByIds(
      revisions.content.mapNotNull { it.authorId }.toSet()
    ).associateBy { it.id }

    val newContent = revisions.content
      .asSequence()
      // map index to sort by it at the end to keep order
      .mapIndexed { index, activityRevision -> index to activityRevision }
      // group by activity managers, so managers can fetch their data one by one
      .groupBy { it.second.type }
      .asSequence()
      // get modifications
      .map { (activityName, indexValuePairs) ->
        val modifications = activityName?.let {
          val activity = activityProvider[activityName] as? Activity
          val revisionIds = indexValuePairs.map { it.second.id }
          activity?.getModifications(revisionIds)
        }
        indexValuePairs.map { Triple(it.first, it.second, modifications?.get(it.second.id)) }
      }
      // get rid of the grouping
      .flatMap { it }
      // construct the entities
      .map { triple ->
        val author = authors[triple.second.authorId]
        triple.first to ProjectActivityView(
          revisionId = triple.second.id,
          timestamp = triple.second.timestamp.time,
          type = triple.second.type ?: "UNKNOWN",
          authorId = triple.second.authorId,
          authorUsername = author?.username,
          authorName = author?.name,
          authorAvatarHash = author?.avatarHash,
          meta = triple.second.meta,
          modifications = triple.third
        )
      }
      // sort it by the index
      .sortedBy { it.first }
      // tadaaa...
      .map { it.second }.toList()

    return PageImpl(newContent, revisions.pageable, revisions.totalElements)
  }

  private fun getProjectActivityRevisions(projectId: Long, pageable: Pageable): Page<ActivityRevision> {
    return activityRevisionRepository.getForProject(projectId, pageable)
  }
}
