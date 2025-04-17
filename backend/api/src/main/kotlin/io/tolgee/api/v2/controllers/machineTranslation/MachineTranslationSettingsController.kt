/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.machineTranslation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.key.LanguageConfigItemModelAssembler
import io.tolgee.dtos.request.SetMachineTranslationSettingsDto
import io.tolgee.hateoas.machineTranslation.LanguageConfigItemModel
import io.tolgee.hateoas.machineTranslation.LanguageInfoModel
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.machineTranslation.MtServiceConfigService
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
@Tag(name = "Machine Translation Settings")
class MachineTranslationSettingsController(
  private val projectHolder: ProjectHolder,
  private val languageConfigItemModelAssembler: LanguageConfigItemModelAssembler,
  private val mtServiceConfigService: MtServiceConfigService,
) {
  @GetMapping("/{projectId}/machine-translation-service-settings")
  @Operation(summary = "Get machine translation settings")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getMachineTranslationSettings(): CollectionModel<LanguageConfigItemModel> {
    val data = mtServiceConfigService.getProjectSettings(projectHolder.projectEntity)
    return languageConfigItemModelAssembler.toCollectionModel(data)
  }

  @PutMapping("/{projectId}/machine-translation-service-settings")
  @Operation(summary = "Sets machine translation settings")
  @RequiresProjectPermissions([Scope.LANGUAGES_EDIT])
  @AllowApiAccess
  fun setMachineTranslationSettings(
    @RequestBody dto: SetMachineTranslationSettingsDto,
  ): CollectionModel<LanguageConfigItemModel> {
    mtServiceConfigService.setProjectSettings(projectHolder.projectEntity, dto)
    return getMachineTranslationSettings()
  }

  @GetMapping("/{projectId}/machine-translation-language-info")
  @Operation(
    summary = "Machine translation info",
    description =
      "Get enabled services " +
        "and configured formality for each language",
  )
  @RequiresProjectPermissions([Scope.LANGUAGES_EDIT])
  @AllowApiAccess
  fun getMachineTranslationLanguageInfo(): CollectionModel<LanguageInfoModel> {
    val data = mtServiceConfigService.getLanguageInfo(projectHolder.project)
    return CollectionModel.of(
      data.map {
        LanguageInfoModel(
          it.language?.id,
          it.language?.tag,
          supportedServices = it.supportedServices,
        )
      }.sortedBy { it.languageId },
    )
  }
}
