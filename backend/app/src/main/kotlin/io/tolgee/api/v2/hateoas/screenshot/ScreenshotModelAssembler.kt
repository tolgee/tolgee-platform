package io.tolgee.api.v2.hateoas.screenshot

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.component.TimestampValidation
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FileStoragePath
import io.tolgee.model.Screenshot
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.*

@Component
class ScreenshotModelAssembler(
  private val timestampValidation: TimestampValidation,
  private val tolgeeProperties: TolgeeProperties,
) : RepresentationModelAssemblerSupport<Screenshot, ScreenshotModel>(
  TranslationsController::class.java, ScreenshotModel::class.java
) {
  override fun toModel(entity: Screenshot): ScreenshotModel {
    val filenameWithSignature = getFilenameWithSignature(entity.filename)
    val thumbnailFilenameWithSignature = getFilenameWithSignature(entity.thumbnailFilename)

    val fileUrl = getFileUrl(filenameWithSignature)
    val thumbnailUrl = getFileUrl(thumbnailFilenameWithSignature)

    return ScreenshotModel(
      id = entity.id,
      filename = filenameWithSignature,
      thumbnail = thumbnailFilenameWithSignature,
      fileUrl = fileUrl,
      thumbnailUrl = thumbnailUrl,
      createdAt = entity.createdAt,
      keyReferences = entity.keyScreenshotReferences.flatMap { reference ->
        val positions = if (reference.positions.isNullOrEmpty())
          listOf(null)
        else
          reference.positions!!

        positions.map { position ->
          KeyInScreenshotModel(
            reference.key.id,
            position,
            reference.key.name,
            reference.key.namespace?.name,
            reference.originalText
          )
        }
      },
      location = entity.location,
      width = entity.width,
      height = entity.height,
    )
      .add(Link.of(fileUrl, "file"))
      .add(Link.of(thumbnailUrl, "thumbnail"))
  }

  private fun getFileUrl(filename: String): String {
    var fileUrl = "${tolgeeProperties.fileStorageUrl}/${FileStoragePath.SCREENSHOTS}/$filename"
    if (!fileUrl.matches(Regex("^https?://.*$"))) {
      val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
      fileUrl = builder.replacePath(fileUrl)
        .replaceQuery("").build().toUriString()
    }
    return fileUrl
  }

  private fun getFilenameWithSignature(filename: String): String {
    var filenameWithSignature = filename
    if (tolgeeProperties.authentication.securedImageRetrieval) {
      filenameWithSignature = "$filenameWithSignature?timestamp=${
      timestampValidation.encryptTimeStamp(filename, Date().time)
      }"
    }
    return filenameWithSignature
  }
}
