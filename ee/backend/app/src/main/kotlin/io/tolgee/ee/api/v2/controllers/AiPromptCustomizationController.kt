package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.data.SetLanguagePromptCustomizationRequest
import io.tolgee.ee.data.SetProjectPromptCustomizationRequest
import io.tolgee.ee.service.AiPromptCustomizationService
import io.tolgee.hateoas.aiPtomptCustomization.ProjectAiPromptCustomizationModel
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.LanguageService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/")
@Suppress("MVCPathVariableInspection")
@Tag(name = "AI Prompt customization")
class AiPromptCustomizationController(
  private val projectHolder: ProjectHolder,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val aiPromptCustomizationService: AiPromptCustomizationService,
  private val languageService: LanguageService,
) {
  @GetMapping("projects/{projectId:[0-9]+}/ai-prompt-customization")
  @Operation(summary = "Returns project level prompt customization")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresProjectPermissions(scopes = [])
  @UseDefaultPermissions
  fun getPromptProjectCustomization(): ProjectAiPromptCustomizationModel {
    return ProjectAiPromptCustomizationModel(
      projectHolder.project.aiTranslatorPromptDescription,
    )
  }

  @PutMapping("projects/{projectId:[0-9]+}/ai-prompt-customization")
  @Operation(summary = "Sets project level prompt customization")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresProjectPermissions(scopes = [Scope.PROJECT_EDIT])
  fun setPromptProjectCustomization(
    @Valid @RequestBody dto: SetProjectPromptCustomizationRequest,
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.AI_PROMPT_CUSTOMIZATION,
    )
    aiPromptCustomizationService.setProjectPromptCustomization(projectHolder.project.id, dto)
  }

  @PutMapping("projects/{projectId:[0-9]+}/languages/{languageId:[0-9]+}/ai-prompt-customization")
  @Operation(summary = "Sets language level prompt customization")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresProjectPermissions(scopes = [Scope.PROJECT_EDIT, Scope.LANGUAGES_EDIT])
  fun setLanguagePromptCustomization(
    @Valid @RequestBody dto: SetLanguagePromptCustomizationRequest,
    @PathVariable languageId: Long,
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.AI_PROMPT_CUSTOMIZATION,
    )
    aiPromptCustomizationService.setLanguagePromptCustomization(projectHolder.project.id, languageId, dto)
  }

  @GetMapping("projects/{projectId:[0-9]+}/language-ai-prompt-customizations")
  @Operation(summary = "Sets project level prompt customization")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresProjectPermissions(scopes = [Scope.PROJECT_EDIT, Scope.LANGUAGES_EDIT])
  fun getLanguagePromptCustomization(
    @Valid @RequestBody dto: SetLanguagePromptCustomizationRequest,
    @PathVariable languageId: Long,
  ) {
  }
}
