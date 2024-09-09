package io.tolgee.service

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.MaxUploadedFilesByUserProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.constants.Message
import io.tolgee.dtos.request.ImageUploadInfoDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.UploadedImage
import io.tolgee.model.UserAccount
import io.tolgee.repository.UploadedImageRepository
import io.tolgee.util.ImageConverter
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.core.io.InputStreamSource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

@Service
class ImageUploadService(
  @Lazy
  val uploadedImageRepository: UploadedImageRepository,
  val fileStorage: FileStorage,
  val dateProvider: CurrentDateProvider,
  val maxUploadedFilesByUserProvider: MaxUploadedFilesByUserProvider,
) {
  val logger = LoggerFactory.getLogger(ImageUploadService::class.java)

  companion object {
    const val UPLOADED_IMAGES_STORAGE_FOLDER_NAME = "uploadedImages"
  }

  @Transactional
  fun store(
    image: InputStreamSource,
    userAccount: UserAccount,
    info: ImageUploadInfoDto?,
  ): UploadedImage {
    if (uploadedImageRepository.countAllByUserAccount(userAccount) > maxUploadedFilesByUserProvider()) {
      throw BadRequestException(Message.TOO_MANY_UPLOADED_IMAGES)
    }

    val imageConverter = ImageConverter(image.inputStream)
    val dimension = imageConverter.originalDimension

    val uploadedImageEntity =
      UploadedImage(generateFilename(), userAccount)
        .apply {
          extension = "png"
          originalWidth = dimension.width
          originalHeight = dimension.height
          width = imageConverter.targetDimension.width
          height = imageConverter.targetDimension.height
          location = info?.location
        }

    save(uploadedImageEntity)
    val processedImage = imageConverter.getImage()
    val processedThumbnail = imageConverter.getThumbnail()

    fileStorage.storeFile(uploadedImageEntity.filePath, processedImage.toByteArray())
    fileStorage.storeFile(uploadedImageEntity.thumbnailFilePath, processedThumbnail.toByteArray())

    return uploadedImageEntity
  }

  @Transactional
  fun delete(uploadedImage: UploadedImage) {
    fileStorage.deleteFile(uploadedImage.filePath)
    uploadedImageRepository.delete(uploadedImage)
  }

  fun find(ids: Collection<Long>): List<UploadedImage> {
    return uploadedImageRepository.findAllByIdIn(ids)
  }

  @Transactional
  @Scheduled(fixedRate = 60000)
  fun cleanOldImages() {
    logger.debug("Clearing images")
    val time = dateProvider.date.toInstant().minus(2, ChronoUnit.HOURS)
    uploadedImageRepository.findAllOlder(Date.from(time)).let { images ->
      images.forEach { delete(it) }
    }
  }

  private fun generateFilename(): String {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return ThreadLocalRandom.current()
      .ints(100L, 0, charPool.size)
      .asSequence()
      .map(charPool::get)
      .joinToString("")
  }

  fun save(image: UploadedImage): UploadedImage {
    return uploadedImageRepository.save(image)
  }

  fun validateIsImage(image: MultipartFile) {
    val contentTypes = listOf("image/png", "image/jpeg", "image/gif")
    if (!contentTypes.contains(image.contentType!!)) {
      throw ValidationException(Message.FILE_NOT_IMAGE)
    }
  }

  val UploadedImage.filePath
    get() = "$UPLOADED_IMAGES_STORAGE_FOLDER_NAME/" + this.filenameWithExtension
  val UploadedImage.thumbnailFilePath
    get() = "$UPLOADED_IMAGES_STORAGE_FOLDER_NAME/" + this.thumbnailFilenameWithExtension
}
