package io.tolgee.repository

import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.key.screenshotReference.KeyScreenshotReferenceId
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface KeyScreenshotReferenceRepository : JpaRepository<KeyScreenshotReference, KeyScreenshotReferenceId> {
  fun getAllByScreenshot(screenshot: Screenshot): List<KeyScreenshotReference>

  @Query(
    """
    FROM KeyScreenshotReference ksr
    WHERE ksr.screenshot.id IN :screenshotIds
  """,
  )
  fun findAll(screenshotIds: Collection<Long>): List<KeyScreenshotReference>

  @Query(
    """
    from KeyScreenshotReference ksr
    where ksr.key = :key and ksr.screenshot.id in :screenshotIds
  """,
  )
  fun findAll(
    key: Key,
    screenshotIds: List<Long>,
  ): List<KeyScreenshotReference>

  @Query(
    """
    from KeyScreenshotReference ksr
    left join fetch ksr.screenshot
    where ksr.key.id in :keyIds
  """,
  )
  fun getAllByKeyIdIn(keyIds: Collection<Long>): List<KeyScreenshotReference>
}
