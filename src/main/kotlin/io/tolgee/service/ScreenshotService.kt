/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import com.amazonaws.services.s3.AmazonS3
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.repository.ScreenshotRepository
import org.springframework.core.io.InputStreamSource
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import kotlin.math.floor
import kotlin.math.sqrt


@Service
open class ScreenshotService(
        private val screenshotRepository: ScreenshotRepository,
        private val tolgeeProperties: TolgeeProperties,
        private val s3: AmazonS3?,
        private val fileStorageService: FileStorageService
) {
    private val screenshotFolderName = "screenshots"

    @Transactional
    open fun store(screenshotImage: InputStreamSource, key: Key): Screenshot {
        val screenshotEntity = Screenshot(key = key)
        screenshotRepository.save(screenshotEntity)
        val image = prepareImage(screenshotImage.inputStream)
        fileStorageService.storeFile(screenshotEntity.getFilePath(), image.toByteArray())
        return screenshotEntity
    }

    open fun findAll(key: Key, pageRequest: PageRequest? = null): List<Screenshot> {
        return screenshotRepository.findAllByKey(key, pageRequest)
    }

    @Transactional
    open fun delete(screenshots: Collection<Screenshot>) {
        screenshots.forEach {
            screenshotRepository.deleteById(it.id!!)
            deleteFile(it)
        }
    }

    open fun findByIdIn(ids: Collection<Long>): MutableList<Screenshot> {
        return screenshotRepository.findAllById(ids)
    }

    open fun deleteAllByRepository(projectId: Long) {
        val all = screenshotRepository.getAllByKeyProjectId(projectId)
        all.forEach { this.deleteFile(it) }
        screenshotRepository.deleteInBatch(all)
    }

    open fun deleteAllByKeyId(keyId: Long) {
        val all = screenshotRepository.getAllByKeyId(keyId)
        all.forEach { this.deleteFile(it) }
        screenshotRepository.deleteInBatch(all)
    }

    open fun deleteAllByKeyId(keyIds: Collection<Long>) {
        val all = screenshotRepository.getAllByKeyIdIn(keyIds)
        all.forEach { this.deleteFile(it) }
        screenshotRepository.deleteInBatch(all)
    }

    private fun prepareImage(screenshotStream: InputStream): ByteArrayOutputStream {
        val image = ImageIO.read(screenshotStream)
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
        param.compressionQuality = 0.5f

        writer.output = imageOutputStream
        writer.write(null, IIOImage(resizedImage, null, null), param)

        outputStream.close()
        imageOutputStream.close()
        writer.dispose()
        return outputStream
    }

    private fun deleteFile(screenshot: Screenshot) {
        fileStorageService.deleteFile(screenshot.getFilePath())
    }

    private fun Screenshot.getFilePath(): String {
        return "${this@ScreenshotService.screenshotFolderName}/${this.filename}"
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
}
