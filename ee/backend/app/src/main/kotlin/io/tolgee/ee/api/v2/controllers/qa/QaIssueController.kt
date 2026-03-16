package io.tolgee.ee.api.v2.controllers.qa

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.qa.QaIssueModelAssembler
import io.tolgee.ee.data.qa.QaCheckIssueIgnoreRequest
import io.tolgee.ee.service.qa.QaIssueService
import io.tolgee.hateoas.qa.QaIssueModel
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springframework.hateoas.CollectionModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/projects/{projectId:[0-9]+}/translations/{translationId:[0-9]+}/qa-issues")
@OpenApiUnstableOperationExtension
@Tag(name = "QA Issues")
class QaIssueController(
  private val qaIssueService: QaIssueService,
  private val qaIssueModelAssembler: QaIssueModelAssembler,
) {
  @GetMapping
  @Operation(summary = "Get persisted QA issues for a translation")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun getIssues(
    @PathVariable projectId: Long,
    @PathVariable translationId: Long,
  ): CollectionModel<QaIssueModel> {
    val issues = qaIssueService.getIssuesForTranslation(projectId, translationId)
    return qaIssueModelAssembler.toCollectionModel(issues)
  }

  @PutMapping("/{issueId}/ignore")
  @Operation(summary = "Ignore a QA issue")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun ignoreIssue(
    @PathVariable projectId: Long,
    @PathVariable translationId: Long,
    @PathVariable issueId: Long,
  ) {
    qaIssueService.ignoreIssue(projectId, issueId)
  }

  @PutMapping("/{issueId}/unignore")
  @Operation(summary = "Unignore a QA issue")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun unignoreIssue(
    @PathVariable projectId: Long,
    @PathVariable translationId: Long,
    @PathVariable issueId: Long,
  ) {
    qaIssueService.unignoreIssue(projectId, issueId)
  }

  @PostMapping("/suppressions")
  @Operation(summary = "Create a QA issue suppression by match parameters")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun createSuppression(
    @PathVariable projectId: Long,
    @PathVariable translationId: Long,
    @RequestBody @Valid request: QaCheckIssueIgnoreRequest,
  ) {
    qaIssueService.ignoreIssueByParams(projectId, translationId, request)
  }

  @DeleteMapping("/suppressions")
  @Operation(summary = "Remove a QA issue suppression by match parameters")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun removeSuppression(
    @PathVariable projectId: Long,
    @PathVariable translationId: Long,
    @RequestBody @Valid request: QaCheckIssueIgnoreRequest,
  ): ResponseEntity<Void> {
    val changed = qaIssueService.unignoreIssueByParams(projectId, translationId, request)
    return if (changed) {
      ResponseEntity.ok().build()
    } else {
      ResponseEntity.noContent().build()
    }
  }
}
