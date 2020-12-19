/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.controllers

import io.polygloat.component.TimestampValidation
import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.constants.Message
import io.polygloat.dtos.request.validators.exceptions.ValidationException
import io.polygloat.service.FileStorageService
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/screenshots"])
class ScreenshotStorageController(
        private val polygloatProperties: PolygloatProperties,
        private val fileStorageService: FileStorageService,
        private val timestampValidation: TimestampValidation
) {
    @GetMapping(value = ["/**"])
    fun getFile(
            request: HttpServletRequest,
            @RequestParam("timestamp") timestamp: String?,
            response: HttpServletResponse
    ): ByteArray {
        if (polygloatProperties.authentication.securedScreenshotRetrieval) {
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