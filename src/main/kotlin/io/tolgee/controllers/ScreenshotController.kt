/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.TimestampValidation
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.GetScreenshotsByKeyDTO
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.dtos.response.ScreenshotDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.model.Screenshot
import io.tolgee.security.repository_auth.AccessWithAnyRepositoryPermission
import io.tolgee.service.KeyService
import io.tolgee.service.RepositoryService
import io.tolgee.service.ScreenshotService
import io.tolgee.service.SecurityService
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/api/repository/screenshots", "/api/repository/{repositoryId:[0-9]+}/screenshots"])
@Tag(name = "Screenshots")
class ScreenshotController(
        private val screenshotService: ScreenshotService,
        private val keyService: KeyService,
        private val repositoryService: RepositoryService,
        private val securityService: SecurityService,
        private val tolgeeProperties: TolgeeProperties,
        private val timestampValidation: TimestampValidation
) {
    @PostMapping("")
    @Operation(summary = "Upload screenshot for specific key")
    fun uploadScreenshot(@PathVariable("repositoryId") repositoryId: Long,
                         @RequestParam("screenshot") screenshot: MultipartFile,
                         @NotBlank @RequestParam key: String): ScreenshotDTO {

        val contentTypes = listOf("image/png", "image/jpeg", "image/gif")

        if (!contentTypes.contains(screenshot.contentType!!)) {
            throw ValidationException(Message.FILE_NOT_IMAGE)
        }

        repositoryService.get(repositoryId).orElseThrow { NotFoundException() }
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.TRANSLATE)
        val keyEntity = keyService.get(repositoryId, PathDTO.fromFullPath(key)).orElseThrow { NotFoundException() }
        val screenShotEntity = screenshotService.store(screenshot, keyEntity)
        return screenShotEntity.toDTO()
    }

    @PostMapping("/get")
    @Operation(summary = "Returns all screenshots for specific key")
    @AccessWithAnyRepositoryPermission
    fun getKeyScreenshots(@PathVariable("repositoryId") repositoryId: Long,
                          @RequestBody @Valid dto: GetScreenshotsByKeyDTO): List<ScreenshotDTO> {
        val keyEntity = keyService.get(repositoryId, PathDTO.fromFullPath(dto.key)).orElseThrow { NotFoundException() }
        return screenshotService.findAll(keyEntity).map { it.toDTO() }
    }

    @DeleteMapping("/{ids}")
    @Operation(summary = "Deletes multiple screenshots by id")
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
        if (tolgeeProperties.authentication.securedScreenshotRetrieval) {
            filename = filename + "?timestamp=" + timestampValidation.encryptTimeStamp(Date().time)
        }
        return ScreenshotDTO(id = entity.id!!, filename = filename, createdAt = entity.createdAt!!)
    }
}
