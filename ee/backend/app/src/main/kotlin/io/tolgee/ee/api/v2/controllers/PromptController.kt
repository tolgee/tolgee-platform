package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.dtos.response.prompt.PromptResponseDto
import io.tolgee.ee.api.v2.hateoas.assemblers.PromptModelAssembler
import io.tolgee.ee.data.prompt.VariablesResponseDto
import io.tolgee.ee.service.prompt.PromptServiceEeImpl
import io.tolgee.ee.service.prompt.PromptVariablesService
import io.tolgee.hateoas.prompt.PromptModel
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.AiPlaygroundResultService
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
@OpenApiOrderExtension(6)
class PromptController(
  private val promptService: PromptServiceEeImpl,
  private val promptModelAssembler: PromptModelAssembler,
  private val arrayResourcesAssembler: PagedResourcesAssembler<Prompt>,
  private val projectHolder: ProjectHolder,
  private val promptVariablesService: PromptVariablesService,
  private val aiPlaygroundResultService: AiPlaygroundResultService,
  private val authenticationFacade: AuthenticationFacade,
) {
  @GetMapping("")
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  fun getAllPaged(
    @ParameterObject pageable: Pageable,
    @RequestParam search: String?,
  ): PagedModel<PromptModel> {
    val result = promptService.getAllPaged(projectHolder.project.id, pageable, search)
    return arrayResourcesAssembler.toModel(result, promptModelAssembler)
  }

  @GetMapping("default")
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  fun getDefaultPrompt(): PromptDto {
    return promptService.getDefaultPrompt()
  }

  @PostMapping("")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  fun createPrompt(
    @RequestBody @Valid dto: PromptDto,
  ): PromptModel {
    val result = promptService.createPrompt(projectHolder.project.id, dto)
    return promptModelAssembler.toModel(result)
  }

  @GetMapping("/{promptId}")
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  fun getPrompt(
    @PathVariable promptId: Long,
  ): PromptModel {
    val result = promptService.findPrompt(projectHolder.project.id, promptId)
    return promptModelAssembler.toModel(result)
  }

  @PutMapping("/{promptId}")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  fun updatePrompt(
    @PathVariable promptId: Long,
    @RequestBody @Valid dto: PromptDto,
  ): PromptModel {
    val result = promptService.updatePrompt(projectHolder.project.id, promptId, dto)
    return promptModelAssembler.toModel(result)
  }

  @DeleteMapping("/{promptId}")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  fun deletePrompt(
    @PathVariable promptId: Long,
  ) {
    promptService.deletePrompt(projectHolder.project.id, promptId)
  }

  @PostMapping("run")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  fun run(
    @Valid @RequestBody promptRunDto: PromptRunDto,
  ): PromptResponseDto {
    val projectId = projectHolder.project.id
    val userId = authenticationFacade.authenticatedUser.id
    val prompt = promptService.getPrompt(projectId, promptRunDto)
    val params = promptService.getLLMParamsFromPrompt(prompt, promptRunDto.keyId)
    val organizationId = projectHolder.project.organizationOwnerId
    val response =
      promptService.runPrompt(
        organizationId,
        params,
        promptRunDto.provider,
        LLMProviderPriority.HIGH,
      )
    val translation = response.parsedJson?.get("output")?.asText()
    if (translation != null) {
      val contextDescription = response.parsedJson?.get("contextDescription")?.asText()
      aiPlaygroundResultService.removeResults(
        projectHolder.project.id,
        userId,
      )
      aiPlaygroundResultService.setResult(
        projectHolder.project.id,
        userId,
        promptRunDto.keyId,
        promptRunDto.targetLanguageId,
        translation,
        contextDescription,
      )
    }
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
      promptVariablesService.getVariables(projectHolder.project.id, keyId, targetLanguageId)
        .map { it.toPromptVariableDto() }.toMutableList(),
    )
  }
}
