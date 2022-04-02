package io.tolgee.repository

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
    from ActivityRevision ar where ar.projectId = :projectId and ar.type is not null
  """
  )
  fun getForProject(projectId: Long, pageable: Pageable): Page<ActivityRevision>

  @Query(
    """
      from ActivityModifiedEntity ame 
      where ame.activityRevision.id in :ids
    """
  )
  fun getModificationsForEachRevision(ids: Collection<Long>): List<ActivityModifiedEntity>
}
