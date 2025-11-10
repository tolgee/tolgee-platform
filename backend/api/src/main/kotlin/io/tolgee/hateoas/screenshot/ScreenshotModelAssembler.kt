package io.tolgee.hateoas.screenshot

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FileStoragePath
import io.tolgee.model.Screenshot
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.JwtService
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Component
class ScreenshotModelAssembler(
  private val tolgeeProperties: TolgeeProperties,
  private val authenticationFacade: AuthenticationFacade,
  private val projectHolder: ProjectHolder,
  private val jwtService: JwtService,
) : RepresentationModelAssemblerSupport<Screenshot, ScreenshotModel>(
    TranslationsController::class.java,
    ScreenshotModel::class.java,
  ) {
  override fun toModel(entity: Screenshot): ScreenshotModel {
    val filenameWithSignature = getFilenameWithSignature(entity.filename)
    val middleSizedFilenameWithSignature = entity.middleSizedFilename?.let { getFilenameWithSignature(it) }
    val thumbnailFilenameWithSignature = getFilenameWithSignature(entity.thumbnailFilename)

    val fileUrl = getFileUrl(filenameWithSignature)
    val middleSizedUrl = middleSizedFilenameWithSignature?.let { getFileUrl(it) }
    val thumbnailUrl = getFileUrl(thumbnailFilenameWithSignature)

    return ScreenshotModel(
      id = entity.id,
      filename = filenameWithSignature,
      middleSized = middleSizedFilenameWithSignature,
      thumbnail = thumbnailFilenameWithSignature,
      fileUrl = fileUrl,
      middleSizedUrl = middleSizedUrl,
      thumbnailUrl = thumbnailUrl,
      createdAt = entity.createdAt,
      keyReferences =
        entity.keyScreenshotReferences.flatMap { reference ->
          val positions =
            if (reference.positions.isNullOrEmpty()) {
              listOf(null)
            } else {
              reference.positions!!
            }

          positions.map { position ->
            KeyInScreenshotModel(
              reference.key.id,
              position,
              reference.key.name,
              reference.key.namespace?.name,
              reference.originalText,
            )
          }
        },
      location = entity.location,
      width = entity.width,
      height = entity.height,
    ).add(Link.of(fileUrl, "file"))
      .add(Link.of(thumbnailUrl, "thumbnail"))
  }

  private fun getFileUrl(filename: String): String {
    var fileUrl = "${tolgeeProperties.fileStorageUrl}/${FileStoragePath.SCREENSHOTS}/$filename"
    if (!fileUrl.matches(Regex("^https?://.*$"))) {
      val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
      fileUrl =
        builder
          .replacePath(fileUrl)
          .replaceQuery("")
          .build()
          .toUriString()
    }
    return fileUrl
  }

  private fun getFilenameWithSignature(filename: String): String {
    var filenameWithSignature = filename
    if (tolgeeProperties.authentication.securedImageRetrieval) {
      val token =
        jwtService.emitTicket(
          authenticationFacade.authenticatedUser.id,
          JwtService.TicketType.IMG_ACCESS,
          tolgeeProperties.authentication.securedImageTimestampMaxAge,
          mapOf(
            "fileName" to filename,
            "projectId" to projectHolder.projectOrNull?.id?.toString(),
          ),
        )

      filenameWithSignature = "$filenameWithSignature?token=$token"
    }

    return filenameWithSignature
  }
}
