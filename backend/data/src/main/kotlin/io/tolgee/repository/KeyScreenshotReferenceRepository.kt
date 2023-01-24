package io.tolgee.repository

import io.tolgee.model.Screenshot
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.key.screenshotReference.KeyScreenshotReferenceId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KeyScreenshotReferenceRepository : JpaRepository<KeyScreenshotReference, KeyScreenshotReferenceId> {

  fun getAllByScreenshot(screenshot: Screenshot): List<KeyScreenshotReference>
}
