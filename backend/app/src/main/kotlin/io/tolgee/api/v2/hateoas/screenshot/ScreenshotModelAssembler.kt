package io.tolgee.api.v2.hateoas.screenshot

import io.tolgee.api.v2.controllers.translation.V2TranslationsController
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
  V2TranslationsController::class.java, ScreenshotModel::class.java
) {
  override fun toModel(entity: Screenshot): ScreenshotModel {
    var filename = entity.filename
    if (tolgeeProperties.authentication.securedImageRetrieval) {
      filename = filename + "?timestamp=" + timestampValidation.encryptTimeStamp(Date().time)
    }

    var fileUrl = "${tolgeeProperties.fileStorageUrl}/${FileStoragePath.SCREENSHOTS}/$filename"
    if (!fileUrl.matches(Regex("^https?://.*$"))) {
      val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
      fileUrl = builder.replacePath(fileUrl)
        .replaceQuery("").build().toUriString()
    }

    return ScreenshotModel(
      id = entity.id,
      filename = filename,
      fileUrl = fileUrl,
      createdAt = entity.createdAt
    ).add(Link.of(fileUrl, "file"))
  }
}
