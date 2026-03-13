package io.tolgee.ee.api.v2.controllers.qa

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.qa.QaCheckCategory
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/projects/{projectId:[0-9]+}/qa-check-types")
@OpenApiUnstableOperationExtension
@Tag(name = "QA Check Types")
class QaCheckTypesController {
  @GetMapping
  @Operation(summary = "Get QA check types grouped by category")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun getCheckTypes(): List<QaCheckCategoryModel> {
    return QaCheckType.CATEGORIES.map { (category, checkTypes) ->
      QaCheckCategoryModel(category = category, checkTypes = checkTypes)
    }
  }
}

// TODO: model must go to a separate file. Also we need to create an assembler for it.
data class QaCheckCategoryModel(
  val category: QaCheckCategory,
  val checkTypes: List<QaCheckType>,
)
