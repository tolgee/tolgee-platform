/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.tolgee.component.TimestampValidation
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.service.FileStorageService
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/screenshots"])
class ScreenshotStorageController(
  private val tolgeeProperties: TolgeeProperties,
  private val fileStorageService: FileStorageService,
  private val timestampValidation: TimestampValidation
) {
  @GetMapping(value = ["/**"])
  fun getFile(
    request: HttpServletRequest,
    @RequestParam("timestamp") timestamp: String?,
    response: HttpServletResponse
  ): ByteArray {
    if (tolgeeProperties.authentication.securedScreenshotRetrieval) {
      if (timestamp == null) {
        throw ValidationException(Message.INVALID_TIMESTAMP)
      }
      timestampValidation.checkTimeStamp(timestamp)
    }
    response.addHeader("Cache-Control", "max-age=365, must-revalidate, no-transform")

    val name = request.requestURI.split(request.contextPath + "/screenshots/")[1]

    return fileStorageService.readFile("screenshots/$name")
  }
}
