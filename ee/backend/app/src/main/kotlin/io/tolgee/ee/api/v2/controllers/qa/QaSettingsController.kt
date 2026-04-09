package io.tolgee.ee.api.v2.controllers.qa

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.qa.LanguageQaConfigModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.qa.QaCheckCategoryModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.qa.QaLanguageSettingsModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.qa.QaSettingsModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.qa.LanguageQaConfigModel
import io.tolgee.ee.api.v2.hateoas.model.qa.QaCheckCategoryModel
import io.tolgee.ee.api.v2.hateoas.model.qa.QaLanguageSettingsModel
import io.tolgee.ee.api.v2.hateoas.model.qa.QaSettingsModel
import io.tolgee.ee.data.qa.QaEnabledRequest
import io.tolgee.ee.data.qa.QaLanguageSettingsRequest
import io.tolgee.ee.data.qa.QaSettingsRequest
import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
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
  private val qaSettingsModelAssembler: QaSettingsModelAssembler,
  private val qaLanguageSettingsModelAssembler: QaLanguageSettingsModelAssembler,
  private val qaCheckCategoryModelAssembler: QaCheckCategoryModelAssembler,
) {
  @GetMapping("/check-types")
  @Operation(summary = "Get QA check types grouped by category")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun getCheckTypes(): List<QaCheckCategoryModel> {
    return QaCheckType.CATEGORIES.map { (category, checkTypes) ->
      qaCheckCategoryModelAssembler.toModel(category, checkTypes)
    }
  }

  @GetMapping("")
  @Operation(summary = "Get QA check settings for the project")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun getSettings(): QaSettingsModel {
    return qaSettingsModelAssembler.toModel(
      projectQaConfigService.getSettings(projectHolder.project.id),
    )
  }

  @PutMapping("")
  @Operation(summary = "Update QA check settings for the project")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun updateSettings(
    @RequestBody @Valid
    dto: QaSettingsRequest,
  ): QaSettingsModel {
    projectQaConfigService.updateSettings(projectHolder.project.id, dto.settings)
    return qaSettingsModelAssembler.toModel(
      projectQaConfigService.getSettings(projectHolder.project.id),
    )
  }

  @PutMapping("/enabled")
  @Operation(summary = "Enable or disable QA checks for the project")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun setQaEnabled(
    @RequestBody @Valid
    dto: QaEnabledRequest,
  ) {
    projectQaConfigService.setQaEnabled(projectHolder.project.id, dto.enabled)
  }

  @GetMapping("/languages")
  @Operation(summary = "Get per-language QA settings overrides for all project languages")
  @ReadOnlyOperation
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
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun getLanguageSettingsResolved(
    @PathVariable languageId: Long,
  ): QaSettingsModel {
    return qaSettingsModelAssembler.toModel(
      projectQaConfigService.getResolvedSettingsForLanguage(projectHolder.project.id, languageId),
    )
  }

  @GetMapping("/languages/{languageId:[0-9]+}")
  @Operation(summary = "Get QA settings overrides for a specific language")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.QA_CHECKS)
  fun getLanguageSettings(
    @PathVariable languageId: Long,
  ): QaLanguageSettingsModel {
    return qaLanguageSettingsModelAssembler.toModel(
      projectQaConfigService.getSettingsForLanguage(projectHolder.project.id, languageId),
    )
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
  ): QaLanguageSettingsModel {
    val projectId = projectHolder.project.id
    projectQaConfigService.updateLanguageSettings(projectId, languageId, dto.settings)
    return qaLanguageSettingsModelAssembler.toModel(
      projectQaConfigService.getSettingsForLanguage(projectId, languageId),
    )
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
