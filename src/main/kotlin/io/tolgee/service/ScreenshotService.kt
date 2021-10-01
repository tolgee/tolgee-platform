/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.repository.ScreenshotRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.ImageUploadService.Companion.UPLOADED_IMAGES_STORAGE_FOLDER_NAME
import org.springframework.core.io.InputStreamSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScreenshotService(
  private val screenshotRepository: ScreenshotRepository,
  private val fileStorageService: FileStorageService,
  private val tolgeeProperties: TolgeeProperties,
  private val imageUploadService: ImageUploadService,
  private val authenticationFacade: AuthenticationFacade
) {
  companion object {
    const val SCREENSHOTS_STORAGE_FOLDER_NAME = "screenshots"
  }

  @Transactional
  fun store(screenshotImage: InputStreamSource, key: Key): Screenshot {
    if (getScreenshotsCountForKey(key) >= tolgeeProperties.maxScreenshotsPerKey) {
      throw BadRequestException(
        Message.MAX_SCREENSHOTS_EXCEEDED,
        listOf(tolgeeProperties.maxScreenshotsPerKey)
      )
    }
    val image = imageUploadService.prepareImage(screenshotImage.inputStream)
    return storeProcessed(image.toByteArray(), key)
  }

  fun storeProcessed(image: ByteArray, key: Key): Screenshot {
    val screenshotEntity = Screenshot(key = key)
    screenshotRepository.save(screenshotEntity)
    fileStorageService.storeFile(screenshotEntity.getFilePath(), image)
    key.screenshots.add(screenshotEntity)
    return screenshotEntity
  }

  @Transactional
  fun saveUploadedImages(uploadedImageIds: Collection<Long>, key: Key) {
    val images = imageUploadService.find(uploadedImageIds)
    if (images.size < uploadedImageIds.size) {
      throw NotFoundException(Message.ONE_OR_MORE_IMAGES_NOT_FOUND)
    }
    images.forEach { uploadedImageEntity ->
      if (authenticationFacade.userAccount.id != uploadedImageEntity.userAccount.id) {
        throw PermissionException()
      }
      val data = fileStorageService
        .readFile(
          UPLOADED_IMAGES_STORAGE_FOLDER_NAME + "/" + uploadedImageEntity.filenameWithExtension
        )
      storeProcessed(data, key)
      imageUploadService.delete(uploadedImageEntity)
    }
  }

  fun findAll(key: Key): List<Screenshot> {
    return screenshotRepository.findAllByKey(key)
  }

  @Transactional
  fun delete(screenshots: Collection<Screenshot>) {
    screenshots.forEach {
      screenshotRepository.deleteById(it.id)
      deleteFile(it)
    }
  }

  fun findByIdIn(ids: Collection<Long>): MutableList<Screenshot> {
    return screenshotRepository.findAllById(ids)
  }

  fun deleteAllByProject(projectId: Long) {
    val all = screenshotRepository.getAllByKeyProjectId(projectId)
    all.forEach { this.deleteFile(it) }
    screenshotRepository.deleteInBatch(all)
  }

  fun deleteAllByKeyId(keyId: Long) {
    val all = screenshotRepository.getAllByKeyId(keyId)
    all.forEach { this.deleteFile(it) }
    screenshotRepository.deleteInBatch(all)
  }

  fun deleteAllByKeyId(keyIds: Collection<Long>) {
    val all = screenshotRepository.getAllByKeyIdIn(keyIds)
    all.forEach { this.deleteFile(it) }
    screenshotRepository.deleteInBatch(all)
  }

  private fun deleteFile(screenshot: Screenshot) {
    fileStorageService.deleteFile(screenshot.getFilePath())
  }

  private fun Screenshot.getFilePath(): String {
    return "$SCREENSHOTS_STORAGE_FOLDER_NAME/${this.filename}"
  }

  fun saveAll(screenshots: List<Screenshot>) {
    screenshotRepository.saveAll(screenshots)
  }

  fun getScreenshotsCountForKey(key: Key): Long {
    return screenshotRepository.countByKey(key)
  }
}
