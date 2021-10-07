package io.tolgee.repository

import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ScreenshotRepository : JpaRepository<Screenshot, Long> {
  fun findAllByKey(key: Key): List<Screenshot>
  fun getAllByKeyProjectId(projectId: Long): List<Screenshot>
  fun getAllByKeyId(id: Long): List<Screenshot>
  fun getAllByKeyIdIn(keyIds: Collection<Long>): List<Screenshot>
  fun countByKey(key: Key): Long

  @Query(
    """
    from Key k join fetch k.screenshots where k.id in :keyIds
  """
  )
  fun getKeysWithScreenshots(keyIds: Collection<Long>): List<Key>
}
