/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FileStoragePath
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.JwtService
import io.tolgee.service.ImageUploadService.Companion.UPLOADED_IMAGES_STORAGE_FOLDER_NAME
import io.tolgee.service.key.ScreenshotService.Companion.SCREENSHOTS_STORAGE_FOLDER_NAME
import io.tolgee.service.security.SecurityService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = [""])
class ImageStorageController(
  private val tolgeeProperties: TolgeeProperties,
  private val fileStorage: FileStorage,
  private val jwtService: JwtService,
  private val securityService: SecurityService,
) {
  @GetMapping(value = ["/${FileStoragePath.SCREENSHOTS}/**"])
  fun getScreenshot(
    request: HttpServletRequest,
    @RequestParam("token") token: String?,
    response: HttpServletResponse,
  ): ByteArray {
    return getFile(
      urlPathPrefix = FileStoragePath.SCREENSHOTS,
      storageFolderName = SCREENSHOTS_STORAGE_FOLDER_NAME,
      token,
      request,
      response,
    )
  }

  @GetMapping(value = ["/${FileStoragePath.UPLOADED_IMAGES}/**"])
  fun getUploadedImage(
    request: HttpServletRequest,
    @RequestParam("token") token: String?,
    response: HttpServletResponse,
  ): ByteArray {
    return getFile(
      urlPathPrefix = FileStoragePath.UPLOADED_IMAGES,
      storageFolderName = UPLOADED_IMAGES_STORAGE_FOLDER_NAME,
      token,
      request,
      response,
    )
  }

  @GetMapping(value = ["/${FileStoragePath.AVATARS}/*"])
  fun getAvatar(
    request: HttpServletRequest,
    response: HttpServletResponse,
  ): ByteArray {
    val fileName = getFileName(request, urlPathPrefix = "avatars")

    return getFile(
      fileName = fileName,
      storageFolderName = FileStoragePath.AVATARS,
      response,
    )
  }

  private fun getFile(
    urlPathPrefix: String,
    storageFolderName: String,
    token: String?,
    request: HttpServletRequest,
    response: HttpServletResponse,
  ): ByteArray {
    val fileName = getFileName(request, urlPathPrefix)

    if (tolgeeProperties.authentication.securedImageRetrieval) {
      // Security consideration: pretend it doesn't exist if we're unauthenticated.
      if (token == null) throw NotFoundException()

      val auth = jwtService.validateTicket(token, JwtService.TicketType.IMG_ACCESS)

      // A token is valid for a single file only
      val authFileName = auth.data?.get("fileName") ?: throw NotFoundException()
      if (authFileName != fileName) throw NotFoundException()

      // A token may be tied to a project in particular
      val authProjectId = auth.data?.get("projectId")?.toLong()
      if (authProjectId != null) {
        // Here, we're accessing the file from within a project with permission from the one who sent us the link.
        // If this person lost permission to see the screenshot, we should not be allowed to access it anymore.
        try {
          securityService.checkProjectPermissionNoApiKey(
            authProjectId,
            Scope.SCREENSHOTS_VIEW,
            auth.userAccount,
          )
        } catch (e: PermissionException) {
          // Security consideration: pretend it doesn't exist if we don't have permission to see it.
          throw NotFoundException()
        }
      }
    }

    return getFile(
      fileName = fileName,
      response = response,
      storageFolderName = storageFolderName,
    )
  }

  private fun getFile(
    fileName: String,
    storageFolderName: String,
    response: HttpServletResponse,
  ): ByteArray {
    response.addHeader("Cache-Control", "max-age=365, must-revalidate, no-transform")
    return fileStorage.readFile("$storageFolderName/$fileName")
  }

  private fun getFileName(
    request: HttpServletRequest,
    urlPathPrefix: String,
  ): String {
    return request.requestURI.split(request.contextPath + "/$urlPathPrefix/")[1]
  }
}
