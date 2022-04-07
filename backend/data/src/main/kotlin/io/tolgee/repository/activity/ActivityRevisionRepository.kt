package io.tolgee.repository.activity

import io.tolgee.activity.ActivityType
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ActivityRevisionRepository : JpaRepository<ActivityRevision, Long> {
  @Query(
    """
    from ActivityRevision ar
    where ar.projectId = :projectId and ar.type is not null
  """
  )
  fun getForProject(projectId: Long, pageable: Pageable): Page<ActivityRevision>

  @Query(
    """
      from ActivityModifiedEntity ame 
      join ame.activityRevision ar
      where ar.id in :ids and 
      ar.type in :allowedActivityTypes
    """
  )
  fun getModificationsByRevisionIdIn(
    ids: Collection<Long>,
    allowedActivityTypes: List<ActivityType>
  ): List<ActivityModifiedEntity>

  @Query(
    """
      select dr
      from ActivityRevision ar 
      join ar.describingRelations dr
      where ar.id in :revisionIds
      and ar.type in :allowedTypes
    """
  )
  fun getRelationsForRevisions(
    revisionIds: List<Long>,
    allowedTypes: Collection<ActivityType>
  ): List<ActivityDescribingEntity>

  @Query(
    """
      select ar.id, me.entityClass, count(me)
      from ActivityRevision ar 
      join ar.modifiedEntities me
      where ar.id in :revisionIds
      and ar.type in :allowedTypes
      group by ar.id, me.entityClass
    """
  )
  fun getModifiedEntityTypeCounts(revisionIds: List<Long>, allowedTypes: Collection<ActivityType>): List<Array<Any>>
}
