package io.tolgee.service.dataImport

import io.tolgee.constants.Message
import io.tolgee.dtos.request.KeyDefinitionDto
import io.tolgee.dtos.request.ScreenshotInfoDto
import io.tolgee.dtos.request.key.KeyScreenshotDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.key.Key
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.service.ImageUploadService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.security.SecurityService
import io.tolgee.util.nullIfEmpty
import org.springframework.context.ApplicationContext

class ScreenshotImporter(
  applicationContext: ApplicationContext,
) {
  private val imageUploadService = applicationContext.getBean(ImageUploadService::class.java)
  private val securityService = applicationContext.getBean(SecurityService::class.java)
  private val screenshotService = applicationContext.getBean(ScreenshotService::class.java)

  fun importScreenshots(
    screenshots: List<ScreenshotToImport>,
    existingKeys: List<Key>,
    addKeyToSave: (namespace: String?, key: String) -> Key,
    newKeys: MutableList<Key>,
    projectId: Long,
  ) {
    if (screenshots.isEmpty()) {
      return
    }
    val uploadedImagesIds = screenshots.map { it.screenshot.uploadedImageId }
    val images = imageUploadService.find(uploadedImagesIds)
    securityService.checkImageUploadPermissions(projectId, images)
    val createdScreenshots =
      images.associate {
        it.id to screenshotService.saveScreenshot(it)
      }

    val locations = images.map { it.location }

    val allReferences =
      screenshotService
        .getKeyScreenshotReferences(
          existingKeys,
          locations,
        ).toMutableList()

    val referencesToDelete = mutableListOf<KeyScreenshotReference>()

    screenshots.forEach {
      val screenshot = it.screenshot
      val key =
        newKeys.find { key -> key.name == it.key.name && key.namespace?.name == it.key.namespace?.nullIfEmpty }
          ?: existingKeys.find { key ->
            key.name == it.key.name && key.namespace?.name == it.key.namespace?.nullIfEmpty
          } ?: addKeyToSave(it.key.namespace?.nullIfEmpty, it.key.name)

      val screenshotResult =
        createdScreenshots[screenshot.uploadedImageId]
          ?: throw NotFoundException(Message.ONE_OR_MORE_IMAGES_NOT_FOUND)
      val info = ScreenshotInfoDto(screenshot.text, screenshot.positions)

      screenshotService.addReference(
        key = key,
        screenshot = screenshotResult.screenshot,
        info = info,
        originalDimension = screenshotResult.originalDimension,
        targetDimension = screenshotResult.targetDimension,
      )

      val toDelete =
        allReferences.filter { reference ->
          reference.key.id == key.id &&
            reference.screenshot.location == screenshotResult.screenshot.location
        }
      referencesToDelete.addAll(toDelete)
    }

    screenshotService.removeScreenshotReferences(referencesToDelete)
  }

  companion object {
    data class ScreenshotToImport(
      val key: KeyDefinitionDto,
      val screenshot: KeyScreenshotDto,
    )
  }
}
