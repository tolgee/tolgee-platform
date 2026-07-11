package io.tolgee.repository

import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ScreenshotRepository : JpaRepository<Screenshot, Long> {
  @Query(
    """FROM Screenshot s where s.id in
      (select ksr.screenshot.id from KeyScreenshotReference ksr where ksr.key = :key)
   """,
  )
  fun findAllByKey(key: Key): List<Screenshot>

  @Query(
    """FROM Screenshot s join fetch s.keyScreenshotReferences ksr
    where s.id in (select ksr.screenshot.id from Key k join k.keyScreenshotReferences ksr where k.project.id = :projectId)
    """,
  )
  fun getAllByKeyProjectId(projectId: Long): List<Screenshot>

  @Query(
    """SELECT count(s.id) FROM Screenshot s where s.id in
    (select ksr.screenshot.id from KeyScreenshotReference ksr where ksr.key = :key)""",
  )
  fun countByKey(key: Key): Long

  @Query(
    """
    from Key k
      join fetch k.keyScreenshotReferences ksr
      join fetch ksr.screenshot s
      left join fetch k.namespace n
    where k.id in :keyIds
    order by k.id, ksr.screenshot.id
  """,
  )
  fun getKeysWithScreenshots(keyIds: Collection<Long>): List<Key>

  @Query(
    """
    from Screenshot s 
      join fetch s.keyScreenshotReferences ksr
    where s.id in :ids
  """,
  )
  fun findAllById(ids: Collection<Long>): List<Screenshot>

  @Query(
    """
    from KeyScreenshotReference ksr
    join fetch ksr.key k
    join fetch ksr.screenshot s
    where k in :keys
    and s.location in :locations
  """,
  )
  fun getKeyScreenshotReferences(
    keys: List<Key>,
    locations: List<String?>,
  ): List<KeyScreenshotReference>

  @Query(
    """
    from KeyScreenshotReference ksr
    join fetch ksr.key k
    join fetch ksr.screenshot s
    where k = :key
  """,
  )
  fun getAllKeyScreenshotReferences(key: Key): List<KeyScreenshotReference>

  /**
   * Returns screenshots that will become orphans after all key_screenshot_reference
   * rows for [branchId]'s keys are deleted — i.e., screenshots referenced ONLY by branch keys
   * and by no key on any other branch.
   *
   * Uses JPQL (not nativeQuery) because Screenshot.filename is a Kotlin computed property,
   * not a DB column — so it cannot be selected in native SQL.
   */
  @Query(
    """
    SELECT s FROM Screenshot s
    WHERE s.id IN (
      SELECT ksr.screenshot.id FROM KeyScreenshotReference ksr
      WHERE ksr.key.id IN (SELECT k.id FROM Key k WHERE k.branch.id = :branchId)
    )
    AND s.id NOT IN (
      SELECT ksr.screenshot.id FROM KeyScreenshotReference ksr
      WHERE ksr.key.id NOT IN (SELECT k.id FROM Key k WHERE k.branch.id = :branchId)
    )
    """,
  )
  fun findOrphansByBranchId(
    @Param("branchId") branchId: Long,
  ): List<Screenshot>
}
