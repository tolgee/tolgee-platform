package io.polygloat.service

import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.model.Key
import io.polygloat.model.Screenshot
import io.polygloat.repository.ScreenshotRepository
import org.springframework.core.io.InputStreamSource
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
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
        private val polygloatProperties: PolygloatProperties
) {
    @Transactional
    open fun store(screenshot: InputStreamSource, key: Key): Screenshot {
        val screenshotEntity = Screenshot(key = key)
        screenshotRepository.save(screenshotEntity)
        val image = prepareImage(screenshot.inputStream)

        val file = File("${polygloatProperties.dataPath}/screenshots/${screenshotEntity.filename}")

        file.parentFile.mkdirs()
        file.writeBytes(image.toByteArray())
        return screenshotEntity
    }

    open fun findAll(key: Key, pageRequest: PageRequest? = null): List<Screenshot> {
        return screenshotRepository.findAllByKey(key, pageRequest)
    }

    @Transactional
    open fun delete(screenshots: Collection<Screenshot>) {
        screenshots.forEach {
            screenshotRepository.deleteById(it.id!!)
            File("${polygloatProperties.dataPath}/screenshots/${it.filename}").delete()
        }
    }

    open fun findByIdIn(ids: Collection<Long>): MutableList<Screenshot> {
        return screenshotRepository.findAllById(ids)
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

    private fun getTargetDimension(image: BufferedImage): Dimension {
        val imagePxs = image.height * image.width
        val maxPxs = 3000000
        val newHeight = floor(sqrt(maxPxs.toDouble()*image.height/image.width)).toInt()
        val newWidth = floor(sqrt(maxPxs.toDouble()*image.width/image.height)).toInt()

        if (imagePxs > maxPxs) {
            return Dimension(newWidth, newHeight)
        }
        return Dimension(image.width, image.height)
    }
}