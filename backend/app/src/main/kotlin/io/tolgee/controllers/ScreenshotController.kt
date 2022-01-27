/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.TimestampValidation
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.screenshot.GetScreenshotsByKeyDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.dtos.response.ScreenshotDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.model.Screenshot
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.service.KeyService
import io.tolgee.service.ProjectService
import io.tolgee.service.ScreenshotService
import io.tolgee.service.SecurityService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/api/project/screenshots", "/api/project/{projectId:[0-9]+}/screenshots"])
@Tag(name = "Screenshots")
@Deprecated("Use V2ScreenshotController")
class ScreenshotController(
  private val screenshotService: ScreenshotService,
  private val keyService: KeyService,
  private val projectService: ProjectService,
  private val securityService: SecurityService,
  private val tolgeeProperties: TolgeeProperties,
  private val timestampValidation: TimestampValidation
) {
  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Upload screenshot for specific key")
  fun uploadScreenshot(
    @PathVariable("projectId") projectId: Long,
    @RequestParam("screenshot") screenshot: MultipartFile,
    @NotBlank @RequestParam key: String
  ): ScreenshotDTO {

    val contentTypes = listOf("image/png", "image/jpeg", "image/gif")

    if (!contentTypes.contains(screenshot.contentType!!)) {
      throw ValidationException(io.tolgee.constants.Message.FILE_NOT_IMAGE)
    }

    projectService.get(projectId)
    securityService.checkProjectPermission(projectId, Permission.ProjectPermissionType.TRANSLATE)
    val keyEntity = keyService.findOptional(projectId, PathDTO.fromFullPath(key)).orElseThrow { NotFoundException() }
    val screenShotEntity = screenshotService.store(screenshot, keyEntity)
    return screenShotEntity.toDTO()
  }

  @PostMapping("/get")
  @Operation(summary = "Returns all screenshots for specific key")
  @AccessWithAnyProjectPermission
  fun getKeyScreenshots(
    @PathVariable("projectId") projectId: Long,
    @RequestBody @Valid dto: GetScreenshotsByKeyDto
  ): List<ScreenshotDTO> {
    val keyEntity = keyService.get(projectId, dto.key)
    return screenshotService.findAll(keyEntity).map { it.toDTO() }
  }

  @DeleteMapping("/{ids}")
  @Operation(summary = "Deletes multiple screenshots by id")
  fun deleteScreenshots(@PathVariable("ids") ids: Set<Long>) {
    val screenshots = screenshotService.findByIdIn(ids)
    screenshots.forEach {
      securityService.checkProjectPermission(
        it.key.project!!.id,
        Permission.ProjectPermissionType.TRANSLATE
      )
    }
    screenshotService.delete(screenshots)
  }

  private fun Screenshot.toDTO(): ScreenshotDTO {
    val entity = this
    var filename = entity.filename
    if (tolgeeProperties.authentication.securedImageRetrieval) {
      filename = filename + "?timestamp=" + timestampValidation.encryptTimeStamp(Date().time)
    }
    return ScreenshotDTO(id = entity.id, filename = filename, createdAt = entity.createdAt!!)
  }
}
