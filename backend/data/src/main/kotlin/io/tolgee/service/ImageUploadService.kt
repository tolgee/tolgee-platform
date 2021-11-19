package io.tolgee.service

import io.tolgee.component.CurrentDateProvider
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.UploadedImage
import io.tolgee.model.UserAccount
import io.tolgee.repository.UploadedImageRepository
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamSource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.streams.asSequence

@Service
class ImageUploadService(
  val uploadedImageRepository: UploadedImageRepository,
  val fileStorageService: FileStorageService,
  val dateProvider: CurrentDateProvider
) {
  val logger = LoggerFactory.getLogger(ImageUploadService::class.java)

  companion object {
    const val UPLOADED_IMAGES_STORAGE_FOLDER_NAME = "uploadedImages"
  }

  @Transactional
  fun store(image: InputStreamSource, userAccount: UserAccount): UploadedImage {
    if (uploadedImageRepository.countAllByUserAccount(userAccount) > 100L) {
      throw BadRequestException(io.tolgee.constants.Message.TOO_MANY_UPLOADED_IMAGES)
    }

    val uploadedImageEntity = UploadedImage(generateFilename(), userAccount)

    save(uploadedImageEntity)
    val processedImage = prepareImage(image.inputStream)
    fileStorageService.storeFile(uploadedImageEntity.filePath, processedImage.toByteArray())
    return uploadedImageEntity
  }

  @Transactional
  fun delete(uploadedImage: UploadedImage) {
    fileStorageService.deleteFile(uploadedImage.filePath)
    uploadedImageRepository.delete(uploadedImage)
  }

  fun find(ids: Collection<Long>): List<UploadedImage> {
    return uploadedImageRepository.findAllByIdIn(ids)
  }

  @Transactional
  @Scheduled(fixedRate = 60000)
  fun cleanOldImages() {
    logger.debug("Clearing images")
    val time = dateProvider.getDate().toInstant().minus(2, ChronoUnit.HOURS)
    uploadedImageRepository.findAllOlder(Date.from(time)).let { images ->
      images.forEach { delete(it) }
    }
  }

  fun prepareImage(imageStream: InputStream, compressionQuality: Float = 0.5f): ByteArrayOutputStream {
    val image = ImageIO.read(imageStream)
    val writer = ImageIO.getImageWritersByFormatName("jpg").next() as ImageWriter
    val targetDimension = getTargetDimension(image)
    val resizedImage = BufferedImage(targetDimension.width, targetDimension.height, BufferedImage.TYPE_INT_RGB)
    val graphics2D: Graphics2D = resizedImage.createGraphics()
    graphics2D.drawImage(image, 0, 0, targetDimension.width, targetDimension.height, null)
    graphics2D.dispose()
    val outputStream = ByteArrayOutputStream()

    val imageOutputStream = ImageIO.createImageOutputStream(outputStream)

    val param = writer.defaultWriteParam
    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
    param.compressionQuality = compressionQuality

    writer.output = imageOutputStream
    writer.write(null, IIOImage(resizedImage, null, null), param)

    outputStream.close()
    imageOutputStream.close()
    writer.dispose()
    return outputStream
  }

  private fun getTargetDimension(image: BufferedImage): Dimension {
    val imagePxs = image.height * image.width
    val maxPxs = 3000000
    val newHeight = floor(sqrt(maxPxs.toDouble() * image.height / image.width)).toInt()
    val newWidth = floor(sqrt(maxPxs.toDouble() * image.width / image.height)).toInt()

    if (imagePxs > maxPxs) {
      return Dimension(newWidth, newHeight)
    }
    return Dimension(image.width, image.height)
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

  val UploadedImage.filePath
    get() = "$UPLOADED_IMAGES_STORAGE_FOLDER_NAME/" + this.filenameWithExtension
}
