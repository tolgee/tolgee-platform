package io.tolgee.repository.activity

import io.tolgee.model.activity.ActivityGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ActivityGroupRepository : JpaRepository<ActivityGroup, Long> {
  @Query(
    nativeQuery = true,
    value = """
      SELECT
        ag.id,
        ag.type,
        ag.matching_string,
        MAX(ar.timestamp) AS last_activity,
        MIN(ar.timestamp) AS first_activity
      FROM activity_group ag
      left JOIN activity_revision_activity_groups arag ON arag.activity_groups_id = ag.id
      left JOIN activity_revision ar ON ar.id = arag.activity_revisions_id
      WHERE
        ag.project_id = :projectId
        AND ag.author_id = :authorId
        AND ag.type = :groupTypeName
        AND (ag.matching_string = :matchingString or (:matchingString is null))
      GROUP BY ag.id
      order by ag.id desc
      limit 1
    """,
  )
  fun findLatest(
    groupTypeName: String,
    authorId: Long?,
    projectId: Long?,
    matchingString: String?,
  ): List<Array<Any?>>
}
