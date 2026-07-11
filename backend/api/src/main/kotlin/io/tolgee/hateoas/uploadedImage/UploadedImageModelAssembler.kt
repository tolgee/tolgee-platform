package io.tolgee.hateoas.uploadedImage

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FileStoragePath
import io.tolgee.model.UploadedImage
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.JwtService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Component
class UploadedImageModelAssembler(
  private val tolgeeProperties: TolgeeProperties,
  private val authenticationFacade: AuthenticationFacade,
  private val projectHolder: ProjectHolder,
  private val jwtService: JwtService,
) : RepresentationModelAssemblerSupport<UploadedImage, UploadedImageModel>(
    TranslationsController::class.java,
    UploadedImageModel::class.java,
  ) {
  override fun toModel(entity: UploadedImage): UploadedImageModel {
    var filename = entity.filenameWithExtension

    if (tolgeeProperties.authentication.securedImageRetrieval) {
      val token =
        jwtService.emitTicket(
          authenticationFacade.authenticatedUser.id,
          JwtService.TicketType.IMG_ACCESS,
          tolgeeProperties.authentication.securedImageTimestampMaxAge,
          mapOf(
            "fileName" to entity.filenameWithExtension,
            "projectId" to projectHolder.projectOrNull?.id?.toString(),
          ),
        )

      filename = "$filename?token=$token"
    }

    var fileUrl = "${tolgeeProperties.fileStorageUrl}/${FileStoragePath.UPLOADED_IMAGES}/$filename"
    if (!fileUrl.matches(Regex("^https?://.*$"))) {
      val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
      fileUrl =
        builder
          .replacePath(fileUrl)
          .replaceQuery("")
          .build()
          .toUriString()
    }

    return UploadedImageModel(
      id = entity.id,
      requestFilename = filename,
      fileUrl = fileUrl,
      filename = entity.filename,
      createdAt = entity.createdAt!!,
      location = entity.location,
    )
  }
}
