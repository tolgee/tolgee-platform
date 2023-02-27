package io.tolgee.api.v2.hateoas.uploadedImage

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.component.TimestampValidation
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FileStoragePath
import io.tolgee.model.UploadedImage
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.*

@Component
class UploadedImageModelAssembler(
  private val timestampValidation: TimestampValidation,
  private val tolgeeProperties: TolgeeProperties
) : RepresentationModelAssemblerSupport<UploadedImage, UploadedImageModel>(
  TranslationsController::class.java, UploadedImageModel::class.java
) {
  override fun toModel(entity: UploadedImage): UploadedImageModel {
    var filename = entity.filenameWithExtension
    if (tolgeeProperties.authentication.securedImageRetrieval) {
      filename = filename + "?timestamp=" + timestampValidation
        .encryptTimeStamp(entity.filenameWithExtension, Date().time)
    }

    var fileUrl = "${tolgeeProperties.fileStorageUrl}/${FileStoragePath.UPLOADED_IMAGES}/$filename"
    if (!fileUrl.matches(Regex("^https?://.*$"))) {
      val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
      fileUrl = builder.replacePath(fileUrl)
        .replaceQuery("").build().toUriString()
    }

    return UploadedImageModel(
      id = entity.id,
      requestFilename = filename,
      fileUrl = fileUrl,
      filename = entity.filename,
      createdAt = entity.createdAt!!,
      location = entity.location
    )
  }
}
