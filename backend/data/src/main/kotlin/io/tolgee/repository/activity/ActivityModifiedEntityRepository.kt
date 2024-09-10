package io.tolgee.repository.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.queryResults.TranslationHistoryView
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityModifiedEntityId
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ActivityModifiedEntityRepository : JpaRepository<ActivityModifiedEntity, ActivityModifiedEntityId> {
  @Query(
    """
    select ame.modifications as modifications, ar.timestamp as timestamp,
    u.name as authorName, u.avatarHash as authorAvatarHash, u.username as authorEmail,
    u.id as authorId, u.deletedAt as authorDeletedAt, ame.revisionType as revisionType
    from ActivityModifiedEntity ame 
    join ame.activityRevision ar
    join UserAccount u on ar.authorId = u.id
    where ame.entityClass = 'Translation' and ame.entityId = :translationId
    and ar.type not in :ignoredActivityTypes
  """,
  )
  fun getTranslationHistory(
    translationId: Long,
    pageable: Pageable,
    ignoredActivityTypes: List<ActivityType>,
  ): Page<TranslationHistoryView>

  @Query(
    """
      from ActivityModifiedEntity ame
        where ame.activityRevision.projectId = :projectId 
        and ame.activityRevision.id in :revisionIds
        and cast(empty_json(ame.modifications) as boolean) = false 
        and (:filterEntityClass is null or ame.entityClass in :filterEntityClass)
        order by ame.activityRevision.id, ame.entityClass, ame.entityId
    """,
  )
  fun getModifiedEntities(
    projectId: Long,
    revisionIds: List<Long>,
    filterEntityClass: List<String>?,
    pageable: Pageable,
  ): Page<ActivityModifiedEntity>
}
