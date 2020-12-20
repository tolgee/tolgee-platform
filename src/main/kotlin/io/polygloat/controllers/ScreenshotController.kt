/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.controllers

import io.polygloat.component.TimestampValidation
import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.constants.Message
import io.polygloat.dtos.PathDTO
import io.polygloat.dtos.request.GetScreenshotsByKeyDTO
import io.polygloat.dtos.request.validators.exceptions.ValidationException
import io.polygloat.dtos.response.ScreenshotDTO
import io.polygloat.exceptions.NotFoundException
import io.polygloat.model.Permission
import io.polygloat.model.Screenshot
import io.polygloat.service.KeyService
import io.polygloat.service.RepositoryService
import io.polygloat.service.ScreenshotService
import io.polygloat.service.SecurityService
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/api/repository/screenshots", "/api/repository/{repositoryId:[0-9]+}/screenshots"])
class ScreenshotController(
        private val screenshotService: ScreenshotService,
        private val keyService: KeyService,
        private val repositoryService: RepositoryService,
        private val securityService: SecurityService,
        private val polygloatProperties: PolygloatProperties,
        private val timestampValidation: TimestampValidation
) {
    @PostMapping("")
    fun uploadScreenshot(@PathVariable("repositoryId") repositoryId: Long,
                         @RequestParam("screenshot") screenshot: MultipartFile,
                         @NotBlank @RequestParam key: String): ScreenshotDTO {

        val contentTypes = listOf("image/png", "image/jpeg", "image/gif")

        if (!contentTypes.contains(screenshot.contentType!!)) {
            throw ValidationException(Message.FILE_NOT_IMAGE)
        }

        repositoryService.findById(repositoryId).orElseThrow { NotFoundException() }
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.TRANSLATE)
        val keyEntity = keyService.get(repositoryId, PathDTO.fromFullPath(key)).orElseThrow { NotFoundException() }
        val screenShotEntity = screenshotService.store(screenshot, keyEntity)
        return screenShotEntity.toDTO()
    }

    @PostMapping("/get")
    fun getKeyScreenshots(@PathVariable("repositoryId") repositoryId: Long,
                          @RequestBody @Valid dto: GetScreenshotsByKeyDTO): List<ScreenshotDTO> {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.VIEW)
        val keyEntity = keyService.get(repositoryId, PathDTO.fromFullPath(dto.key)).orElseThrow { NotFoundException() }
        return screenshotService.findAll(keyEntity).map { it.toDTO() }
    }

    @DeleteMapping("/{ids}")
    fun deleteScreenshots(@PathVariable("ids") ids: Set<Long>) {
        val screenshots = screenshotService.findByIdIn(ids)
        screenshots.forEach {
            securityService.checkRepositoryPermission(
                    it.key.repository!!.id,
                    Permission.RepositoryPermissionType.TRANSLATE
            )
        }
        screenshotService.delete(screenshots)
    }

    private fun Screenshot.toDTO(): ScreenshotDTO {
        val entity = this
        var filename = entity.filename
        if (polygloatProperties.authentication.securedScreenshotRetrieval) {
            filename = filename + "?timestamp=" + timestampValidation.encryptTimeStamp(Date().time)
        }
        return ScreenshotDTO(id = entity.id!!, filename = filename, createdAt = entity.createdAt)
    }
}