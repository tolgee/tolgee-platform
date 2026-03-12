package io.tolgee.ee.api.v2.controllers.qa

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.qa.LanguageQaConfigModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.qa.LanguageQaConfigModel
import io.tolgee.ee.data.qa.QaLanguageSettingsRequest
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
import io.tolgee.service.language.LanguageService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
  private val languageService: LanguageService,
  private val languageQaConfigModelAssembler: LanguageQaConfigModelAssembler,
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

  @GetMapping("/languages")
  @Operation(summary = "Get per-language QA settings overrides for all project languages")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun getAllLanguageSettings(): List<LanguageQaConfigModel> {
    val projectId = projectHolder.project.id
    val languages = languageService.findAll(projectId)
    val langConfigs = projectQaConfigService.getSettingsForAllLanguages(projectId)
    val configByLanguageId = langConfigs.associateBy { it.language.id }

    return languages.map { lang ->
      languageQaConfigModelAssembler.toModel(configByLanguageId[lang.id], lang)
    }
  }

  @GetMapping("/languages/{languageId:[0-9]+}/resolved")
  @Operation(summary = "Get resolved QA settings for a specific language")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun getLanguageSettingsResolved(
    @PathVariable languageId: Long,
  ): Map<QaCheckType, QaCheckSeverity> {
    return projectQaConfigService.getResolvedSettingsForLanguage(projectHolder.project.id, languageId)
  }

  @GetMapping("/languages/{languageId:[0-9]+}")
  @Operation(summary = "Get QA settings overrides for a specific language")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun getLanguageSettings(
    @PathVariable languageId: Long,
  ): Map<QaCheckType, QaCheckSeverity>? {
    return projectQaConfigService.getSettingsForLanguage(projectHolder.project.id, languageId)
  }

  @PutMapping("/languages/{languageId:[0-9]+}")
  @Operation(summary = "Set per-language QA settings override")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun updateLanguageSettings(
    @PathVariable languageId: Long,
    @RequestBody @Valid
    dto: QaLanguageSettingsRequest,
  ): Map<QaCheckType, QaCheckSeverity>? {
    val projectId = projectHolder.project.id
    projectQaConfigService.updateLanguageSettings(projectId, languageId, dto.settings)
    return projectQaConfigService.getSettingsForLanguage(projectId, languageId)
  }

  @DeleteMapping("/languages/{languageId:[0-9]+}")
  @Operation(summary = "Reset language QA settings to global defaults")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun deleteLanguageSettings(
    @PathVariable languageId: Long,
  ) {
    projectQaConfigService.deleteLanguageSettings(projectHolder.project.id, languageId)
  }
}
