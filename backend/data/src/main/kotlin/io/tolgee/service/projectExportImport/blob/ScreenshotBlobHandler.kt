package io.tolgee.service.projectExportImport.blob

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.model.Screenshot
import io.tolgee.service.key.ScreenshotService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/** Exports the full-size screenshot image (no derived sizes), named by source id. */
@Component
class ScreenshotBlobHandler(
  private val fileStorage: FileStorage,
  private val screenshotService: ScreenshotService,
) : BlobHandler,
  Logging {
  override val entityClass: KClass<*> = Screenshot::class

  override fun export(entity: Any): List<BlobEntry> {
    val screenshot = entity as Screenshot
    val sourcePath = screenshotService.getScreenshotPath(screenshot.filename)
    if (!fileStorage.fileExists(sourcePath)) {
      logger.warn("Screenshot ${screenshot.id} image missing at $sourcePath; exporting without its blob")
      return emptyList()
    }
    return listOf(BlobEntry(blobName(screenshot.id, screenshot.extension), fileStorage.readFile(sourcePath)))
  }

  companion object {
    fun blobName(
      screenshotId: Long,
      extension: String?,
    ): String = "screenshots/$screenshotId.${extension ?: "jpg"}"
  }
}
