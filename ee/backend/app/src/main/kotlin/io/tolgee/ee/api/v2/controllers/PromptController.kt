package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.dtos.response.prompt.PromptResponseDto
import io.tolgee.ee.api.v2.hateoas.assemblers.PromptModelAssembler
import io.tolgee.ee.data.prompt.VariablesResponseDto
import io.tolgee.ee.service.prompt.DefaultPromptHelper
import io.tolgee.ee.service.prompt.PromptServiceEeImpl
import io.tolgee.ee.service.prompt.PromptVariablesHelper
import io.tolgee.hateoas.prompt.PromptModel
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/prompts",
    "/v2/projects/prompts",
  ],
)
@Tag(name = "Ai prompt controller")
@OpenApiOrderExtension(6)
class PromptController(
  private val promptService: PromptServiceEeImpl,
  private val promptModelAssembler: PromptModelAssembler,
  private val arrayResourcesAssembler: PagedResourcesAssembler<Prompt>,
  private val projectHolder: ProjectHolder,
  private val promptVariablesHelper: PromptVariablesHelper,
  private val defaultPromptHelper: DefaultPromptHelper,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val businessEventPublisher: BusinessEventPublisher,
  private val authenticationFacade: AuthenticationFacade,
) {
  @GetMapping("")
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  @Operation(summary = "Get all prompts")
  fun getAllPaged(
    @ParameterObject pageable: Pageable,
    @RequestParam search: String?,
  ): PagedModel<PromptModel> {
    val result = promptService.getAllPaged(projectHolder.project.id, pageable, search)
    return arrayResourcesAssembler.toModel(result, promptModelAssembler)
  }

  @GetMapping("default")
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  @Operation(summary = "Get default prompt")
  fun getDefaultPrompt(): PromptDto {
    return promptService.getDefaultPrompt()
  }

  @PostMapping("")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @Operation(summary = "Create prompt")
  fun createPrompt(
    @RequestBody @Valid dto: PromptDto,
  ): PromptModel {
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "AI_PROMPT_CREATE",
        userAccountDto = authenticationFacade.authenticatedUser,
      ),
    )

    // ai-playground allows custom templates only as paid feature
    if (dto.template != null) {
      enabledFeaturesProvider.checkFeatureEnabled(
        projectHolder.project.organizationOwnerId,
        Feature.AI_PROMPT_CUSTOMIZATION,
      )
    }

    val result = promptService.createPrompt(projectHolder.project.id, dto)
    return promptModelAssembler.toModel(result)
  }

  @GetMapping("/{promptId}")
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  @Operation(summary = "Get prompt by id")
  fun getPrompt(
    @PathVariable promptId: Long,
  ): PromptModel {
    val result = promptService.findPrompt(projectHolder.project.id, promptId)
    return promptModelAssembler.toModel(result)
  }

  @PutMapping("/{promptId}")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @Operation(summary = "Update prompt")
  fun updatePrompt(
    @PathVariable promptId: Long,
    @RequestBody @Valid dto: PromptDto,
  ): PromptModel {
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "AI_PROMPT_UPDATE",
        userAccountDto = authenticationFacade.authenticatedUser,
      ),
    )

    // ai-playground allows custom templates only as paid feature
    if (dto.template != null) {
      enabledFeaturesProvider.checkFeatureEnabled(
        projectHolder.project.organizationOwnerId,
        Feature.AI_PROMPT_CUSTOMIZATION,
      )
    }
    val result = promptService.updatePrompt(projectHolder.project.id, promptId, dto)
    return promptModelAssembler.toModel(result)
  }

  @DeleteMapping("/{promptId}")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @Operation(summary = "Delete prompt")
  fun deletePrompt(
    @PathVariable promptId: Long,
  ) {
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "AI_PROMPT_DELETE",
        userAccountDto = authenticationFacade.authenticatedUser,
      ),
    )

    promptService.deletePrompt(projectHolder.project.id, promptId)
  }

  @PostMapping("run")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @Operation(summary = "Run prompt")
  fun run(
    @Valid @RequestBody data: PromptRunDto,
  ): PromptResponseDto {
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "AI_PROMPT_RUN",
        userAccountDto = authenticationFacade.authenticatedUser,
      ),
    )

    // ai-playground allows custom templates only as paid feature
    if (data.template != null) {
      enabledFeaturesProvider.checkFeatureEnabled(
        projectHolder.project.organizationOwnerId,
        Feature.AI_PROMPT_CUSTOMIZATION,
      )
    }

    val projectId = projectHolder.project.id
    val prompt =
      promptService.getPrompt(
        projectId,
        data.template ?: defaultPromptHelper.getDefaultPrompt().template!!,
        data.keyId,
        data.targetLanguageId,
        data.basicPromptOptions,
      )
    val params = promptService.getLlmParamsFromPrompt(prompt, data.keyId, LlmProviderPriority.HIGH)
    val organizationId = projectHolder.project.organizationOwnerId
    val response =
      promptService.runPromptAndChargeCredits(
        organizationId,
        params,
        data.provider
      )
    return PromptResponseDto(
      prompt,
      response.response,
      response.parsedJson,
      price = response.price,
      usage = response.usage,
    )
  }

  @GetMapping("get-variables")
  @Operation(summary = "Get variables")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  fun variables(
    @RequestParam keyId: Long?,
    @RequestParam targetLanguageId: Long?,
  ): VariablesResponseDto {
    return VariablesResponseDto(
      promptVariablesHelper.getVariables(projectHolder.project.id, keyId, targetLanguageId)
        .map { it.toPromptVariableDto() }.toMutableList(),
    )
  }
}
