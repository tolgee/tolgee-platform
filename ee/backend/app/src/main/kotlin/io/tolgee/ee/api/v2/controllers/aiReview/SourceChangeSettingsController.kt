package io.tolgee.ee.api.v2.controllers.aiReview

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.api.v2.hateoas.model.aiReview.SourceChangeSettingsModel
import io.tolgee.ee.data.aiReview.SourceChangeSettingsRequest
import io.tolgee.ee.service.aiReview.SourceChangeSettingsService
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/projects/{projectId:[0-9]+}/source-change-settings")
@Tag(name = "AI review source change settings")
@OpenApiUnstableOperationExtension
class SourceChangeSettingsController(
  private val sourceChangeSettingsService: SourceChangeSettingsService,
  private val projectHolder: ProjectHolder,
) {
  @GetMapping("")
  @Operation(summary = "Get the source change settings")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @AllowApiAccess
  fun getSourceChangeSettings(): SourceChangeSettingsModel = toModel(projectHolder.project)

  @PutMapping("")
  @Operation(
    summary = "Set the source change settings",
    description = "The setting decides whether human work is overwritten automatically later.",
  )
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @RequestActivity(ActivityType.AI_REVIEW_SETTINGS_UPDATE)
  @AllowApiAccess
  fun setSourceChangeSettings(
    @RequestBody @Valid request: SourceChangeSettingsRequest,
  ): SourceChangeSettingsModel {
    val project =
      sourceChangeSettingsService.update(
        projectHolder.project.id,
        aiApproved = request.aiApproved,
        humanEdited = request.humanEdited,
        humanReviewed = request.humanReviewed,
      )
    return toModel(ProjectDto.fromEntity(project))
  }

  private fun toModel(project: ProjectDto): SourceChangeSettingsModel {
    val effective = sourceChangeSettingsService.getEffective(project)
    val locked = sourceChangeSettingsService.isProtectReviewed(project.id)
    return SourceChangeSettingsModel(
      aiApproved = effective.aiApproved,
      humanEdited = effective.humanEdited,
      humanReviewed = effective.humanReviewed,
      aiApprovedLocked = locked,
      humanReviewedLocked = locked,
    )
  }
}
