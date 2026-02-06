package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.hateoas.batch.BatchTranslateInfoModel
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:\\d+}/batch-translate-info",
    "/v2/projects/batch-translate-info",
  ],
)
@Tag(name = "Batch Operations")
@Suppress("MVCPathVariableInspection")
class BatchTranslateInfoController(
  private val projectHolder: ProjectHolder,
  private val llmProviderService: LlmProviderService,
) {
  @GetMapping("")
  @Operation(
    summary = "Get batch translation info",
    description = "Returns information about batch translation availability and pricing for the project",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  fun getBatchTranslateInfo(): BatchTranslateInfoModel {
    val organizationId = projectHolder.project.organizationOwnerId
    val info = llmProviderService.getBatchInfo(organizationId)

    return BatchTranslateInfoModel(
      available = info.available,
      discountPercent = info.discountPercent,
      userChoiceAllowed = info.userChoiceAllowed,
      providerType = info.providerType,
    )
  }
}
