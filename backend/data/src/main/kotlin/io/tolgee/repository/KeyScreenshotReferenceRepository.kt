package io.tolgee.repository

import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.key.screenshotReference.KeyScreenshotReferenceId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface KeyScreenshotReferenceRepository : JpaRepository<KeyScreenshotReference, KeyScreenshotReferenceId> {

  fun getAllByScreenshot(screenshot: Screenshot): List<KeyScreenshotReference>

  @Query(
    """
    FROM KeyScreenshotReference ksr
    WHERE ksr.screenshot IN :screenshots
  """
  )
  fun getAllByScreenshot(screenshots: List<Screenshot>): List<KeyScreenshotReference>

  @Query(
    """
    from KeyScreenshotReference ksr
    where ksr.key = :key and ksr.screenshot in :screenshots
  """
  )
  fun findAll(key: Key, screenshots: List<Screenshot>): KeyScreenshotReference
}
