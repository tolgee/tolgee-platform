package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.component.machineTranslation.providers.llm.LLMParams
import io.tolgee.dtos.request.prompt.PromptCreateDto
import io.tolgee.dtos.request.prompt.PromptTestDto
import io.tolgee.dtos.request.prompt.VariablesResponse
import io.tolgee.dtos.response.PromptResponseDto
import io.tolgee.hateoas.prompt.PromptModel
import io.tolgee.hateoas.prompt.PromptModelAssembler
import io.tolgee.model.Prompt
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.PromptService
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
  private val promptService: PromptService,
  private val promptModelAssembler: PromptModelAssembler,
  private val arrayResourcesAssembler: PagedResourcesAssembler<Prompt>,
  private val projectHolder: ProjectHolder,
) {
  @GetMapping("")
  @UseDefaultPermissions
  fun getAllPaged(
    @ParameterObject pageable: Pageable,
    @RequestParam search: String?,
  ): PagedModel<PromptModel> {
    val result = promptService.getAllPaged(projectHolder.project.id, pageable, search)
    return arrayResourcesAssembler.toModel(result, promptModelAssembler)
  }

  @PostMapping("")
  @UseDefaultPermissions
  fun createPrompt(
    @RequestBody @Valid dto: PromptCreateDto,
  ): PromptModel {
    val result = promptService.createPrompt(projectHolder.project.id, dto)
    return promptModelAssembler.toModel(result)
  }

  @DeleteMapping("/{promptId}")
  @UseDefaultPermissions
  fun deletePrompt(
    @RequestParam promptId: Long,
  ) {
    promptService.deletePrompt(projectHolder.project.id, promptId)
  }

  @PostMapping("run")
  @UseDefaultPermissions
  fun run(
    @Valid @RequestBody promptTestDto: PromptTestDto,
  ): PromptResponseDto {
    val prompt = promptService.getPrompt(projectHolder.project.id, promptTestDto)
    val messages = promptService.getLlmMessages(prompt, promptTestDto)
    val response = promptService.runPrompt(LLMParams(messages), promptTestDto)
    return PromptResponseDto(
      prompt,
      response.translated ?: "",
      response.usage,
    )
  }

  @GetMapping("get-variables")
  @Operation(summary = "Get variables")
  @UseDefaultPermissions
  fun variables(
    @RequestParam keyId: Long,
    @RequestParam targetLanguageId: Long,
  ): VariablesResponse {
    return VariablesResponse(promptService.getVariables(projectHolder.project.id, keyId, targetLanguageId))
  }
}
