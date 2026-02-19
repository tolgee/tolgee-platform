package io.tolgee.ee.api.v2.controllers.qa

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.qa.QaIssueModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.qa.QaIssueModel
import io.tolgee.ee.service.qa.QaIssueService
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
}
