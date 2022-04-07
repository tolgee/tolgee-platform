/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.ActivityType
import io.tolgee.activity.RequestActivity
import io.tolgee.api.v2.hateoas.screenshot.ScreenshotModel
import io.tolgee.api.v2.hateoas.screenshot.ScreenshotModelAssembler
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.model.Screenshot
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.key.Key
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.KeyService
import io.tolgee.service.ScreenshotService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/keys/{keyId}/screenshots",
    "/v2/projects/{projectId:[0-9]+}/keys/{keyId}/screenshots"
  ]
)
@Tag(name = "Screenshots")
class V2ScreenshotController(
  private val screenshotService: ScreenshotService,
  private val keyService: KeyService,
  private val projectHolder: ProjectHolder,
  private val screenshotModelAssembler: ScreenshotModelAssembler
) {
  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Upload screenshot for specific key")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @AccessWithApiKey([ApiScope.SCREENSHOTS_UPLOAD])
  @ResponseStatus(HttpStatus.CREATED)
  @RequestActivity(ActivityType.SCREENSHOT_ADD)
  fun uploadScreenshot(
    @PathVariable keyId: Long,
    @RequestParam("screenshot") screenshot: MultipartFile,
  ): ResponseEntity<ScreenshotModel> {
    val contentTypes = listOf("image/png", "image/jpeg", "image/gif")
    if (!contentTypes.contains(screenshot.contentType!!)) {
      throw ValidationException(io.tolgee.constants.Message.FILE_NOT_IMAGE)
    }
    val keyEntity = keyService.findOptional(keyId).orElseThrow { NotFoundException() }
    keyEntity.checkInProject()
    val screenShotEntity = screenshotService.store(screenshot, keyEntity)
    return ResponseEntity(screenShotEntity.model, HttpStatus.CREATED)
  }

  @GetMapping("")
  @Operation(summary = "Returns all screenshots for specified key")
  @AccessWithAnyProjectPermission
  @AccessWithApiKey([ApiScope.SCREENSHOTS_VIEW])
  fun getKeyScreenshots(@PathVariable keyId: Long): CollectionModel<ScreenshotModel> {
    val keyEntity = keyService.findOptional(keyId).orElseThrow { NotFoundException() }
    keyEntity.checkInProject()
    return screenshotModelAssembler.toCollectionModel(screenshotService.findAll(keyEntity))
  }

  @DeleteMapping("/{ids}")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @Operation(summary = "Deletes multiple screenshots by ids")
  @AccessWithApiKey([ApiScope.SCREENSHOTS_VIEW])
  @RequestActivity(ActivityType.SCREENSHOT_DELETE)
  fun deleteScreenshots(@PathVariable("ids") ids: Set<Long>) {
    val screenshots = screenshotService.findByIdIn(ids)
    screenshots.forEach {
      it.key.checkInProject()
    }
    screenshotService.delete(screenshots)
  }

  private fun Key.checkInProject() {
    keyService.checkInProject(this, projectHolder.project.id)
  }

  private val Screenshot.model: ScreenshotModel
    get() = screenshotModelAssembler.toModel(this)
}
