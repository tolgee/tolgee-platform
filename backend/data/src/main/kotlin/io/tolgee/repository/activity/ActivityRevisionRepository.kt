package io.tolgee.repository.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.activity.ActivityRevision
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ActivityRevisionRepository : JpaRepository<ActivityRevision, Long> {
  @Query(
    """
    from ActivityRevision ar
    where ar.projectId = :projectId and ar.type is not null and ar.batchJobChunkExecution is null and ar.type in :types
  """,
  )
  fun getForProject(
    projectId: Long,
    pageable: Pageable,
    types: List<ActivityType>,
  ): Page<ActivityRevision>

  @Query(
    """
      select ar.id, me.entityClass, count(me)
      from ActivityRevision ar 
      join ar.modifiedEntities me
      where ar.id in :revisionIds
      and ar.type in :allowedTypes
      group by ar.id, me.entityClass
    """,
  )
  fun getModifiedEntityTypeCounts(
    revisionIds: List<Long>,
    allowedTypes: Collection<ActivityType>,
  ): List<Array<Any>>

  @Query(
    """
      select count(ar.id) as count, function('to_char', ar.timestamp, 'yyyy-MM-dd') as date
      from ActivityRevision ar
      where ar.projectId = :projectId
      group by date
      order by date
    """,
  )
  fun getProjectDailyActivity(projectId: Long): List<Array<Any>>

  @Query(
    """
    from ActivityRevision ar where ar.id = :revisionId and (ar.projectId = :projectId or :projectId is null) 
  """,
  )
  fun find(
    projectId: Long?,
    revisionId: Long,
  ): ActivityRevision?
}
