/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.tolgee.component.TimestampValidation
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FileStoragePath
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.service.ImageUploadService.Companion.UPLOADED_IMAGES_STORAGE_FOLDER_NAME
import io.tolgee.service.ScreenshotService.Companion.SCREENSHOTS_STORAGE_FOLDER_NAME
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = [""])
class ImageStorageController(
  private val tolgeeProperties: TolgeeProperties,
  private val fileStorage: FileStorage,
  private val timestampValidation: TimestampValidation
) {
  @GetMapping(value = ["/${FileStoragePath.SCREENSHOTS}/**"])
  fun getScreenshot(
    request: HttpServletRequest,
    @RequestParam("timestamp") timestamp: String?,
    response: HttpServletResponse
  ): ByteArray {
    return getFile(
      urlPathPrefix = "screenshots",
      storageFolderName = SCREENSHOTS_STORAGE_FOLDER_NAME,
      timestamp,
      request,
      response
    )
  }

  @GetMapping(value = ["/${FileStoragePath.UPLOADED_IMAGES}/**"])
  fun getUploadedImage(
    request: HttpServletRequest,
    @RequestParam("timestamp") timestamp: String?,
    response: HttpServletResponse
  ): ByteArray {
    return getFile(
      urlPathPrefix = FileStoragePath.UPLOADED_IMAGES,
      storageFolderName = UPLOADED_IMAGES_STORAGE_FOLDER_NAME,
      timestamp,
      request,
      response
    )
  }

  @GetMapping(value = ["/${FileStoragePath.AVATARS}/*"])
  fun getAvatar(
    request: HttpServletRequest,
    response: HttpServletResponse
  ): ByteArray {
    return getFile(
      urlPathPrefix = "avatars",
      storageFolderName = FileStoragePath.AVATARS,
      request,
      response
    )
  }

  private fun getFile(
    urlPathPrefix: String,
    storageFolderName: String,
    timestamp: String?,
    request: HttpServletRequest,
    response: HttpServletResponse
  ): ByteArray {
    if (tolgeeProperties.authentication.securedImageRetrieval) {
      if (timestamp == null) {
        throw ValidationException(io.tolgee.constants.Message.INVALID_TIMESTAMP)
      }
      timestampValidation.checkTimeStamp(timestamp)
    }

    return getFile(
      response = response,
      request = request,
      urlPathPrefix = urlPathPrefix,
      storageFolderName = storageFolderName
    )
  }

  private fun getFile(
    urlPathPrefix: String,
    storageFolderName: String,
    request: HttpServletRequest,
    response: HttpServletResponse,
  ): ByteArray {
    response.addHeader("Cache-Control", "max-age=365, must-revalidate, no-transform")

    // since there is a "." character in the URL, we have to parse like this
    val name = request.requestURI.split(request.contextPath + "/$urlPathPrefix/")[1]

    return fileStorage.readFile("$storageFolderName/$name")
  }
}
