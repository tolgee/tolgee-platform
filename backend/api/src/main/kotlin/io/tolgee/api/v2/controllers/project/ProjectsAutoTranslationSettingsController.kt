/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.AutoTranslationSettingsDto
import io.tolgee.hateoas.autoTranslationConfig.AutoTranslationConfigModel
import io.tolgee.hateoas.autoTranslationConfig.AutoTranslationSettingsModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.translation.AutoTranslationService
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress(names = ["MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection"])
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects"])
@Tag(name = "Auto-translation Settings")
class ProjectsAutoTranslationSettingsController(
  private val projectHolder: ProjectHolder,
  private val autoTranslateService: AutoTranslationService,
  private val autoTranslationSettingsModelAssembler: AutoTranslationSettingsModelAssembler,
) {
  @PutMapping("/{projectId:[0-9]+}/per-language-auto-translation-settings")
  @Operation(
    summary = "Set per-language auto-translation settings",
  )
  @RequiresProjectPermissions([Scope.LANGUAGES_EDIT])
  @AllowApiAccess
  fun setPerLanguageAutoTranslationSettings(
    @RequestBody dto: List<AutoTranslationSettingsDto>,
  ): CollectionModel<AutoTranslationConfigModel> {
    val config = autoTranslateService.saveConfig(projectHolder.projectEntity, dto)
    return autoTranslationSettingsModelAssembler.toCollectionModel(config)
  }

  @GetMapping("/{projectId:[0-9]+}/per-language-auto-translation-settings")
  @Operation(summary = "Get per-language auto-translation settings")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getPerLanguageAutoTranslationSettings(): CollectionModel<AutoTranslationConfigModel> {
    val configs = autoTranslateService.getConfigs(projectHolder.projectEntity)
    return autoTranslationSettingsModelAssembler.toCollectionModel(configs)
  }

  @PutMapping("/{projectId:[0-9]+}/auto-translation-settings")
  @Operation(
    summary = "Set default auto translation settings for project",
    description =
      "Sets default auto-translation settings for project " +
        "(deprecated: use per language config with null language id)",
    deprecated = true,
  )
  @RequiresProjectPermissions([Scope.LANGUAGES_EDIT])
  @AllowApiAccess
  @OpenApiHideFromPublicDocs
  fun setAutoTranslationSettings(
    @RequestBody dto: AutoTranslationSettingsDto,
  ): AutoTranslationConfigModel {
    val config = autoTranslateService.saveDefaultConfig(projectHolder.projectEntity, dto)
    return autoTranslationSettingsModelAssembler.toModel(config)
  }

  @GetMapping("/{projectId:[0-9]+}/auto-translation-settings")
  @Operation(
    summary = "Get default auto-translation settings for project",
    description =
      "Returns default auto translation settings for project " +
        "(deprecated: use per language config with null language id)",
    deprecated = true,
  )
  @UseDefaultPermissions
  @AllowApiAccess
  @OpenApiHideFromPublicDocs
  fun getAutoTranslationSettings(): AutoTranslationConfigModel {
    val config = autoTranslateService.getDefaultConfig(projectHolder.projectEntity)
    return autoTranslationSettingsModelAssembler.toModel(config)
  }
}
