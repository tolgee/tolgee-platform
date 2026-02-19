package io.tolgee.ee.api.v2.controllers.qa

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.qa.QaCheckResultModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.qa.QaCheckResultModel
import io.tolgee.ee.data.qa.QaCheckPreviewRequest
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckRunnerService
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.TranslationService
import jakarta.validation.Valid
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/projects/{projectId:[0-9]+}/qa-check")
@OpenApiUnstableOperationExtension
@Tag(name = "QA Check Preview")
class QaCheckPreviewController(
  private val projectHolder: ProjectHolder,
  private val qaCheckRunnerService: QaCheckRunnerService,
  private val modelAssembler: QaCheckResultModelAssembler,
  private val languageService: LanguageService,
  private val translationService: TranslationService,
) {
  @PostMapping("/preview")
  @Operation(summary = "Runs QA checks for the provided text without storing them")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun preview(
    @RequestBody @Valid
    dto: QaCheckPreviewRequest,
  ): CollectionModel<QaCheckResultModel> {
    val params = buildQaCheckParams(dto)
    val results = qaCheckRunnerService.runChecks(params)
    return modelAssembler.toCollectionModel(results)
  }

  private fun buildQaCheckParams(dto: QaCheckPreviewRequest): QaCheckParams {
    val projectId = projectHolder.project.id
    val baseLanguage = languageService.getProjectBaseLanguage(projectId)

    var baseTag: String? = baseLanguage.tag
    if (dto.languageTag == baseTag) {
      baseTag = null
    }

    var baseText: String? = null
    if (baseTag != null && dto.keyId != null) {
      val translations = translationService.getTranslations(listOf(dto.keyId), listOf(baseLanguage.id))
      baseText = translations.firstOrNull()?.text
    }

    return QaCheckParams(
      baseText = baseText,
      text = dto.text,
      baseLanguageTag = baseTag,
      languageTag = dto.languageTag,
    )
  }
}
