package io.tolgee.activity

import io.tolgee.activity.holders.ActivityHolder
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.views.activity.ModifiedEntityView
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.activity.ActivityRevisionRepository
import io.tolgee.service.UserAccountService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManager

@Component
class ActivityService(
  private val entityManager: EntityManager,
  private val activityRevisionRepository: ActivityRevisionRepository,
  private val userAccountService: UserAccountService,
  private val transactionManager: PlatformTransactionManager,
) {

  fun storeData(activityHolder: ActivityHolder) {
    val activityRevision = activityHolder.activityRevision ?: return
    val modifiedEntities = activityHolder.modifiedEntities

    val tt = TransactionTemplate(transactionManager)
    tt.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW

    tt.executeWithoutResult {
      if (activityRevision.id != 0L) {
        entityManager.persist(activityRevision)
      }
      activityRevision.describingRelations.forEach {
        entityManager.persist(it)
      }
      modifiedEntities.values.flatMap { it.values }.forEach { activityModifiedEntity ->
        entityManager.persist(activityModifiedEntity)
      }
    }
  }

//  fun onActivity(activityModifiedEntity: ActivityModifiedEntity) {
//    val tt = TransactionTemplate(transactionManager)
//    tt.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
//    tt.execute {
//      activityDescribingEntityRepository.saveAll(activityModifiedEntity.activityRevision.describingRelations)
//      activityModifiedEntityRepository.save(activityModifiedEntity)
//      activityRevisionRepository.save(activityModifiedEntity.activityRevision)
//    }
//  }

  fun getModifiedEntitiesForEachRevision(
    revisions: Collection<ActivityRevision>,
    describingRelations: Map<Long, List<ActivityDescribingEntity>>
  ): Map<Long, List<ModifiedEntityView>> {
    return activityRevisionRepository
      .getModificationsForEachRevision(revisions.map { it.id })
      .map {
        val relations = it.describingRelations?.map { relationEntry ->
          relationEntry.key to decompressRef(
            relationEntry.value,
            describingRelations[it.activityRevision.id]!!
          )
        }?.toMap()
        ModifiedEntityView(
          activityRevision = it.activityRevision,
          entityClass = it.entityClass,
          entityId = it.entityId,
          modifications = it.modifications,
          description = it.description,
          describingRelations = relations
        )
      }
      .groupBy { it.activityRevision.id }
  }

  private fun decompressRef(
    value: EntityDescriptionRef,
    describingEntities: List<ActivityDescribingEntity>
  ): EntityDescription {
    val entity = describingEntities.find { it.entityClass == value.entityClass && it.entityId == value.entityId }

    val relations = entity?.describingRelations
      ?.map { it.key to decompressRef(it.value, describingEntities) }
      ?.toMap()

    return EntityDescription(
      entityClass = value.entityClass,
      entityId = value.entityId,
      data = entity?.data ?: mapOf(),
      relations = relations ?: mapOf()
    )
  }

  fun getProjectActivity(projectId: Long, pageable: Pageable): Page<ProjectActivityView> {
    val revisions = getProjectActivityRevisions(projectId, pageable)
    val revisionRelations = getRevisionRelations(revisions.map { it.id }.toList())

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
          val revisions = indexValuePairs.map { it.second }
          getModifiedEntitiesForEachRevision(revisions, revisionRelations)
        }
        indexValuePairs.map { (index, activityRevision) ->
          Triple(
            index,
            activityRevision,
            modifications?.get(activityRevision.id)
          )
        }
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

  private fun getRevisionRelations(revisionIds: List<Long>): Map<Long, List<ActivityDescribingEntity>> {
    return activityRevisionRepository.getRelationsForRevisions(revisionIds).groupBy { it.activityRevision.id }
  }

  private fun getProjectActivityRevisions(projectId: Long, pageable: Pageable): Page<ActivityRevision> {
    return activityRevisionRepository.getForProject(projectId, pageable)
  }
}
