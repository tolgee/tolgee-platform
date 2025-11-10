/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Message
import io.tolgee.dtos.request.ScreenshotInfoDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.screenshot.ScreenshotModel
import io.tolgee.hateoas.screenshot.ScreenshotModelAssembler
import io.tolgee.model.Screenshot
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import org.springframework.hateoas.CollectionModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/keys/{keyId}/screenshots",
    "/v2/projects/{projectId:[0-9]+}/keys/{keyId}/screenshots",
  ],
)
@Tag(name = "Screenshots")
class KeyScreenshotController(
  private val screenshotService: ScreenshotService,
  private val keyService: KeyService,
  private val projectHolder: ProjectHolder,
  private val screenshotModelAssembler: ScreenshotModelAssembler,
) {
  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Upload screenshot")
  @ResponseStatus(HttpStatus.CREATED)
  @RequestActivity(ActivityType.SCREENSHOT_ADD)
  @RequestBody(content = [Content(encoding = [Encoding(name = "info", contentType = "application/json")])])
  @RequiresProjectPermissions([Scope.SCREENSHOTS_UPLOAD])
  @AllowApiAccess
  fun uploadScreenshot(
    @PathVariable keyId: Long,
    @RequestPart("screenshot") screenshot: MultipartFile,
    @RequestPart("info", required = false) info: ScreenshotInfoDto?,
  ): ResponseEntity<ScreenshotModel> {
    val contentTypes = listOf("image/png", "image/jpeg", "image/gif")
    if (!contentTypes.contains(screenshot.contentType!!)) {
      throw ValidationException(Message.FILE_NOT_IMAGE)
    }
    val keyEntity = keyService.findOptional(keyId).orElseThrow { NotFoundException() }
    keyEntity.checkInProject()
    val screenShotEntity = screenshotService.store(screenshot, keyEntity, info)
    return ResponseEntity(screenShotEntity.model, HttpStatus.CREATED)
  }

  @GetMapping("")
  @Operation(summary = "Get screenshots")
  @RequiresProjectPermissions([Scope.SCREENSHOTS_VIEW])
  @AllowApiAccess
  fun getKeyScreenshots(
    @PathVariable keyId: Long,
  ): CollectionModel<ScreenshotModel> {
    val keyEntity = keyService.findOptional(keyId).orElseThrow { NotFoundException() }
    keyEntity.checkInProject()
    return screenshotModelAssembler.toCollectionModel(screenshotService.findAll(keyEntity))
  }

  @DeleteMapping("/{ids}")
  @Operation(summary = "Delete screenshots")
  @RequestActivity(ActivityType.SCREENSHOT_DELETE)
  @RequiresProjectPermissions([Scope.SCREENSHOTS_DELETE])
  @AllowApiAccess
  fun deleteScreenshots(
    @PathVariable("ids") ids: Set<Long>,
    @PathVariable keyId: Long,
  ) {
    val screenshots = screenshotService.findByIdIn(ids)
    screenshots.forEach {
      it.checkInProject()
    }
    val key = keyService.get(keyId)
    key.checkInProject()
    screenshotService.removeScreenshotReferences(key, screenshots)
  }

  private fun Screenshot.checkInProject() {
    this.keyScreenshotReferences.forEach {
      if (it.key.project.id != projectHolder.project.id) {
        throw PermissionException(Message.KEY_NOT_FROM_PROJECT)
      }
    }
  }

  private fun Key.checkInProject() {
    keyService.checkInProject(this, projectHolder.project.id)
  }

  private val Screenshot.model: ScreenshotModel
    get() = screenshotModelAssembler.toModel(this)
}
