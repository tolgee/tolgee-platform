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
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/prompts"])
@OpenApiOrderExtension(6)
class PromptController(
  private val promptService: PromptService,
  private val promptModelAssembler: PromptModelAssembler,
  private val arrayResourcesAssembler: PagedResourcesAssembler<Prompt>,
  ) {
  @GetMapping("")
  @UseDefaultPermissions
  fun getAllPaged(
    @ParameterObject pageable: Pageable,
    @PathVariable organizationId: Long,
    @RequestParam search: String?,
  ): PagedModel<PromptModel> {
    val result = promptService.getAllPaged(organizationId, pageable, search)
    return arrayResourcesAssembler.toModel(result, promptModelAssembler)
  }

  @PostMapping("")
  @UseDefaultPermissions
  fun createPrompt(
    @PathVariable organizationId: Long,
    @RequestBody @Valid dto: PromptCreateDto,
  ): PromptModel {
    val result = promptService.createPrompt(organizationId, dto)
    return promptModelAssembler.toModel(result)
  }

  @DeleteMapping("/{promptId}")
  @UseDefaultPermissions
  fun deletePrompt(
    @PathVariable organizationId: Long,
    @RequestParam promptId: Long,
  ) {
    promptService.deletePrompt(organizationId, promptId)
  }

  @PostMapping("run")
  @UseDefaultPermissions
  fun run(
    @PathVariable organizationId: Long,
    @Valid @RequestBody promptTestDto: PromptTestDto,
  ): PromptResponseDto {
    val prompt = promptService.getPrompt(promptTestDto)
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
    @PathVariable organizationId: Long,
    @RequestParam projectId: Long,
    @RequestParam keyId: Long,
    @RequestParam targetLanguageId: Long,
  ): VariablesResponse {
    return VariablesResponse(promptService.getVariables(projectId, keyId, targetLanguageId))
  }
}
