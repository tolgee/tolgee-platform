package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.LanguageAiPromptCustomizationModelAssembler
import io.tolgee.ee.data.SetLanguagePromptCustomizationRequest
import io.tolgee.ee.data.SetProjectPromptCustomizationRequest
import io.tolgee.ee.service.AiPromptCustomizationService
import io.tolgee.hateoas.aiPtomptCustomization.LanguageAiPromptCustomizationModel
import io.tolgee.hateoas.aiPtomptCustomization.ProjectAiPromptCustomizationModel
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiEeExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.language.LanguageService
import jakarta.validation.Valid
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/")
@Suppress("MVCPathVariableInspection")
@Tag(name = "AI Customization")
@OpenApiEeExtension
class AiPromptCustomizationController(
  private val projectHolder: ProjectHolder,
  private val aiPromptCustomizationService: AiPromptCustomizationService,
  private val languageService: LanguageService,
  private val languageAiPromptCustomizationModelAssembler: LanguageAiPromptCustomizationModelAssembler,
) {
  @GetMapping("projects/{projectId:[0-9]+}/ai-prompt-customization")
  @Operation(summary = "Returns project level prompt customization")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
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
  @RequiresFeatures(Feature.AI_PROMPT_CUSTOMIZATION)
  fun setPromptProjectCustomization(
    @Valid @RequestBody dto: SetProjectPromptCustomizationRequest,
  ): ProjectAiPromptCustomizationModel {
    val project = aiPromptCustomizationService.setProjectPromptCustomization(projectHolder.project.id, dto)
    return ProjectAiPromptCustomizationModel(
      project.aiTranslatorPromptDescription,
    )
  }

  @PutMapping("projects/{projectId:[0-9]+}/languages/{languageId:[0-9]+}/ai-prompt-customization")
  @Operation(summary = "Sets language level prompt customization")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresProjectPermissions(scopes = [Scope.PROJECT_EDIT, Scope.LANGUAGES_EDIT])
  @RequiresFeatures(Feature.AI_PROMPT_CUSTOMIZATION)
  fun setLanguagePromptCustomization(
    @Valid @RequestBody dto: SetLanguagePromptCustomizationRequest,
    @PathVariable languageId: Long,
  ): LanguageAiPromptCustomizationModel {
    aiPromptCustomizationService.setLanguagePromptCustomization(projectHolder.project.id, languageId, dto)
    return languageAiPromptCustomizationModelAssembler.toModel(
      languageService.get(
        languageId,
        projectHolder.project.id,
      ),
    )
  }

  @GetMapping("projects/{projectId:[0-9]+}/language-ai-prompt-customizations")
  @Operation(summary = "Returns language level prompt customization")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresProjectPermissions(scopes = [Scope.PROJECT_EDIT, Scope.LANGUAGES_EDIT])
  fun getLanguagePromptCustomizations(): CollectionModel<LanguageAiPromptCustomizationModel> {
    val languages =
      languageService
        .getProjectLanguages(projectHolder.project.id)
        .filter {
          !it.base
        }.sortedBy {
          it.tag
        }
    return languageAiPromptCustomizationModelAssembler.toCollectionModel(languages)
  }
}
