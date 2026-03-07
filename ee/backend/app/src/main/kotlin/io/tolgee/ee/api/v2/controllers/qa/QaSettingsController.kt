package io.tolgee.ee.api.v2.controllers.qa

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.data.qa.QaSettingsRequest
import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/projects/{projectId:[0-9]+}/qa-settings")
@OpenApiUnstableOperationExtension
@Tag(name = "QA Settings")
class QaSettingsController(
  private val projectHolder: ProjectHolder,
  private val projectQaConfigService: ProjectQaConfigService,
) {
  @GetMapping("")
  @Operation(summary = "Get QA check settings for the project")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun getSettings(): Map<QaCheckType, QaCheckSeverity> {
    // TODO: don't use map directly - wrap it in model
    return projectQaConfigService.getSettings(projectHolder.project.id)
  }

  @PutMapping("")
  @Operation(summary = "Update QA check settings for the project")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun updateSettings(
    @RequestBody @Valid
    dto: QaSettingsRequest,
  ): Map<QaCheckType, QaCheckSeverity> {
    projectQaConfigService.updateSettings(projectHolder.project.id, dto.settings)
    return projectQaConfigService.getSettings(projectHolder.project.id)
  }
}
