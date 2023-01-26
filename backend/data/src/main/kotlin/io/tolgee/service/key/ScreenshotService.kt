/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.key

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.key.screenshotReference.KeyScreenshotReferenceId
import io.tolgee.repository.KeyScreenshotReferenceRepository
import io.tolgee.repository.ScreenshotRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import io.tolgee.service.ImageUploadService.Companion.UPLOADED_IMAGES_STORAGE_FOLDER_NAME
import io.tolgee.util.ImageConverter
import org.springframework.core.io.InputStreamSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
class ScreenshotService(
  private val screenshotRepository: ScreenshotRepository,
  private val fileStorage: FileStorage,
  private val tolgeeProperties: TolgeeProperties,
  private val imageUploadService: ImageUploadService,
  private val authenticationFacade: AuthenticationFacade,
  private val entityManager: EntityManager,
  private val keyScreenshotReferenceRepository: KeyScreenshotReferenceRepository
) {
  companion object {
    const val SCREENSHOTS_STORAGE_FOLDER_NAME = "screenshots"
  }

  @Transactional
  fun store(screenshotImage: InputStreamSource, key: Key): Screenshot {
    if (getScreenshotsCountForKey(key) >= tolgeeProperties.maxScreenshotsPerKey) {
      throw BadRequestException(
        io.tolgee.constants.Message.MAX_SCREENSHOTS_EXCEEDED,
        listOf(tolgeeProperties.maxScreenshotsPerKey)
      )
    }
    val converter = ImageConverter(screenshotImage.inputStream)
    val image = converter.getImage()
    val thumbnail = converter.getThumbNail()
    return storeProcessed(image.toByteArray(), thumbnail.toByteArray(), key)
  }

  fun storeProcessed(image: ByteArray, thumbnail: ByteArray, key: Key): Screenshot {
    val screenshot = Screenshot()
    screenshot.extension = "png"
    val reference = KeyScreenshotReference()
    reference.key = key
    reference.screenshot = screenshot
    screenshot.keyScreenshotReferences.add(reference)
    key.keyScreenshotReferences.add(reference)
    entityManager.persist(reference)
    screenshotRepository.save(screenshot)
    fileStorage.storeFile(screenshot.getThumbnailPath(), thumbnail)
    fileStorage.storeFile(screenshot.getFilePath(), image)
    return screenshot
  }

  @Transactional
  fun saveUploadedImages(uploadedImageIds: Collection<Long>, key: Key) {
    val images = imageUploadService.find(uploadedImageIds)
    if (images.size < uploadedImageIds.size) {
      throw NotFoundException(io.tolgee.constants.Message.ONE_OR_MORE_IMAGES_NOT_FOUND)
    }
    images.forEach { uploadedImageEntity ->
      if (authenticationFacade.userAccount.id != uploadedImageEntity.userAccount.id) {
        throw PermissionException()
      }
      val data = fileStorage
        .readFile(
          UPLOADED_IMAGES_STORAGE_FOLDER_NAME + "/" + uploadedImageEntity.filenameWithExtension
        )
      val thumbnail = ImageConverter(data.inputStream()).getThumbNail()
      storeProcessed(data, thumbnail.toByteArray(), key)
      imageUploadService.delete(uploadedImageEntity)
    }
  }

  fun findAll(key: Key): List<Screenshot> {
    return screenshotRepository.findAllByKey(key)
  }

  @Transactional
  fun delete(screenshots: Collection<Screenshot>) {
    screenshots.forEach {
      delete(it)
    }
  }

  @Transactional
  fun delete(screenshot: Screenshot) {
    screenshotRepository.delete(screenshot)
    deleteFile(screenshot)
  }

  fun removeScreenshotReference(key: Key, screenshot: Screenshot) {
    val reference = keyScreenshotReferenceRepository
      .getReferenceById(KeyScreenshotReferenceId(key.id, screenshot.id))
    keyScreenshotReferenceRepository.delete(reference)
    val screenshotReferences = keyScreenshotReferenceRepository.getAllByScreenshot(screenshot)
    if (screenshotReferences.isEmpty()) {
      delete(screenshot)
    }
  }

  fun findByIdIn(ids: Collection<Long>): List<Screenshot> {
    return screenshotRepository.findAllById(ids)
  }

  fun deleteAllByProject(projectId: Long) {
    val all = screenshotRepository.getAllByKeyProjectId(projectId)
    all.forEach { this.deleteFile(it) }
    screenshotRepository.deleteAllInBatch(all)
  }

  fun deleteAllByKeyId(keyId: Long) {
    val all = screenshotRepository.getAllByKeyId(keyId)
    all.forEach { this.deleteFile(it) }
    screenshotRepository.deleteAllInBatch(all)
  }

  fun deleteAllByKeyId(keyIds: Collection<Long>) {
    val all = screenshotRepository.getAllByKeyIdIn(keyIds)
    all.forEach { this.deleteFile(it) }
    screenshotRepository.deleteAllInBatch(all)
  }

  private fun deleteFile(screenshot: Screenshot) {
    fileStorage.deleteFile(screenshot.getFilePath())
  }

  private fun Screenshot.getFilePath(): String {
    return "$SCREENSHOTS_STORAGE_FOLDER_NAME/${this.filename}"
  }

  private fun Screenshot.getThumbnailPath(): String {
    return "$SCREENSHOTS_STORAGE_FOLDER_NAME/${this.thumbnailFilename}"
  }

  fun saveAll(screenshots: List<Screenshot>) {
    screenshotRepository.saveAll(screenshots)
  }

  fun getScreenshotsCountForKey(key: Key): Long {
    return screenshotRepository.countByKey(key)
  }

  fun getKeysWithScreenshots(ids: Collection<Long>): List<Key> {
    return screenshotRepository.getKeysWithScreenshots(ids)
  }

  fun saveAllReferences(data: List<KeyScreenshotReference>) {
    keyScreenshotReferenceRepository.saveAll(data)
  }
}
