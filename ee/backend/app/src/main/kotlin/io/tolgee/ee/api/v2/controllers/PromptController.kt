package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.ee.api.v2.hateoas.assemblers.PromptModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.prompt.PromptResponseModel
import io.tolgee.ee.api.v2.hateoas.model.prompt.PromptResponseUsageModelAssembler
import io.tolgee.ee.data.prompt.VariablesResponseDto
import io.tolgee.ee.service.prompt.DefaultPromptHelper
import io.tolgee.ee.service.prompt.PromptResultParser
import io.tolgee.ee.service.prompt.PromptServiceEeImpl
import io.tolgee.ee.service.prompt.PromptVariablesHelper
import io.tolgee.exceptions.AiReviewInvalidOutputException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.aiReview.AiReviewIssueModel
import io.tolgee.hateoas.prompt.PromptModel
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.PromptType
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.aiReview.AiPlaygroundReviewRunner
import io.tolgee.service.aiReview.AiReviewAvailability
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper

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
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val arrayResourcesAssembler: PagedResourcesAssembler<Prompt>,
  private val projectHolder: ProjectHolder,
  private val promptVariablesHelper: PromptVariablesHelper,
  private val defaultPromptHelper: DefaultPromptHelper,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val businessEventPublisher: BusinessEventPublisher,
  private val authenticationFacade: AuthenticationFacade,
  private val promptResponseUsageModelAssembler: PromptResponseUsageModelAssembler,
  private val aiReviewAvailability: AiReviewAvailability,
  private val aiPlaygroundReviewRunner: AiPlaygroundReviewRunner,
  private val objectMapper: ObjectMapper,
) {
  @GetMapping("")
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  @Operation(summary = "Get all prompts")
  fun getAllPaged(
    @ParameterObject pageable: Pageable,
    @RequestParam search: String?,
    @RequestParam type: PromptType?,
  ): PagedModel<PromptModel> {
    val result = promptService.getAllPaged(projectHolder.project.id, pageable, search, visibleType(type))
    return arrayResourcesAssembler.toModel(result, promptModelAssembler)
  }

  @GetMapping("default")
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  @Operation(summary = "Get default prompt")
  fun getDefaultPrompt(
    @RequestParam(required = false, defaultValue = "TRANSLATE") type: PromptType,
  ): PromptDto {
    checkTypeVisible(type)
    return promptService.getDefaultPrompt(type)
  }

  @PostMapping("")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @Operation(summary = "Create prompt")
  @RequestActivity(ActivityType.AI_PROMPT_CREATE)
  fun createPrompt(
    @RequestBody @Valid dto: PromptDto,
  ): PromptModel {
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "AI_PROMPT_CREATE",
        userAccountDto = authenticationFacade.authenticatedUser,
      ),
    )

    checkTypeAvailable(dto.type)

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
    val result = promptService.findPromptWithResolvedProvider(projectHolder.project.id, promptId)
    checkTypeVisible(result.type)
    return promptModelAssembler.toModel(result)
  }

  @PutMapping("/{promptId}")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @Operation(summary = "Update prompt")
  @RequestActivity(ActivityType.AI_PROMPT_UPDATE)
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

    checkTypeVisible(promptService.findPrompt(projectHolder.project.id, promptId).type)
    checkTypeAvailable(dto.type)

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
  @RequestActivity(ActivityType.AI_PROMPT_DELETE)
  fun deletePrompt(
    @PathVariable promptId: Long,
  ) {
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "AI_PROMPT_DELETE",
        userAccountDto = authenticationFacade.authenticatedUser,
      ),
    )

    checkTypeVisible(promptService.findPrompt(projectHolder.project.id, promptId).type)
    promptService.deletePrompt(projectHolder.project.id, promptId)
  }

  @PostMapping("run")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @Operation(summary = "Run prompt")
  fun run(
    @Valid @RequestBody data: PromptRunDto,
  ): PromptResponseModel {
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "AI_PROMPT_RUN",
        userAccountDto = authenticationFacade.authenticatedUser,
      ),
    )

    checkTypeAvailable(data.type)

    // ai-playground allows custom templates only as paid feature
    if (data.template != null) {
      enabledFeaturesProvider.checkFeatureEnabled(
        projectHolder.project.organizationOwnerId,
        Feature.AI_PROMPT_CUSTOMIZATION,
      )
    }

    if (data.type == PromptType.REVIEW) {
      return runReview(data)
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
        data.provider,
      )
    return PromptResponseModel(
      prompt,
      response.promptResult.response,
      response.parsedJson,
      price = response.promptResult.price,
      usage = response.promptResult.usage?.let { promptResponseUsageModelAssembler.toModel(it) },
    )
  }

  private fun runReview(data: PromptRunDto): PromptResponseModel {
    val result =
      try {
        aiPlaygroundReviewRunner.reviewAndStoreResult(
          projectId = projectHolder.project.id,
          userId = authenticationFacade.authenticatedUser.id,
          data = data,
          priority = LlmProviderPriority.HIGH,
        )
      } catch (e: AiReviewInvalidOutputException) {
        return PromptResponseModel(
          prompt = e.prompt ?: "",
          result = e.rawResponse,
          parsedJson = PromptResultParser.extractJson(e.rawResponse, objectMapper),
          price = e.price,
          usage = e.usage?.let { promptResponseUsageModelAssembler.toModel(it) },
          reviewError = Message.AI_REVIEW_INVALID_OUTPUT.code,
        )
      }
    return PromptResponseModel(
      prompt = result.prompt,
      result = result.rawResponse,
      parsedJson = PromptResultParser.extractJson(result.rawResponse, objectMapper),
      price = result.price,
      usage = result.usage?.let { promptResponseUsageModelAssembler.toModel(it) },
      reviewScore = result.score,
      reviewIssues = result.issues.map { AiReviewIssueModel.of(it) },
    )
  }

  private fun visibleType(type: PromptType?): PromptType? {
    if (aiReviewAvailability.isVisible(projectHolder.project.organizationOwnerId)) return type
    return PromptType.TRANSLATE
  }

  private fun checkTypeVisible(type: PromptType) {
    if (type == PromptType.TRANSLATE) return
    if (!aiReviewAvailability.isVisible(projectHolder.project.organizationOwnerId)) {
      throw NotFoundException()
    }
  }

  private fun checkTypeAvailable(type: PromptType) {
    if (type == PromptType.TRANSLATE) return
    aiReviewAvailability.checkAvailable(projectHolder.project.organizationOwnerId)
  }

  @GetMapping("get-variables")
  @Operation(summary = "Get variables")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  fun variables(
    @RequestParam keyId: Long?,
    @RequestParam targetLanguageId: Long?,
  ): VariablesResponseDto {
    return VariablesResponseDto(
      promptVariablesHelper
        .getVariables(projectHolder.project.id, keyId, targetLanguageId)
        .map { it.toPromptVariableDto() }
        .toMutableList(),
    )
  }
}
