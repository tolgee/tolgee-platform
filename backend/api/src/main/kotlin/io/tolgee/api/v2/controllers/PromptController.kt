package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.component.machineTranslation.providers.llm.LLMParams
import io.tolgee.component.machineTranslation.providers.llm.OllamaApiService
import io.tolgee.component.machineTranslation.providers.llm.OpenaiApiService
import io.tolgee.dtos.request.prompt.PromptCreateDto
import io.tolgee.dtos.request.prompt.PromptTestDto
import io.tolgee.dtos.request.prompt.VariablesResponse
import io.tolgee.dtos.response.PromptResponseDto
import io.tolgee.hateoas.prompt.PromptModel
import io.tolgee.hateoas.prompt.PromptModelAssembler
import io.tolgee.model.Prompt
import io.tolgee.openApiDocs.OpenApiOrderExtension
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
  private val openaiApiService: OpenaiApiService,
  private val ollamaApiService: OllamaApiService,
  private val promptModelAssembler: PromptModelAssembler,
  private val arrayResourcesAssembler: PagedResourcesAssembler<Prompt>,
  ) {
  @GetMapping("")
  fun getAllPaged(
    @ParameterObject
    pageable: Pageable,
    @RequestParam
    organizationId: Long,
    @RequestParam
    search: String?,
  ): PagedModel<PromptModel> {
    val result = promptService.getAllPaged(organizationId, pageable, search)
    return arrayResourcesAssembler.toModel(result, promptModelAssembler)
  }

  @PostMapping("")
  fun createPrompt(
    @RequestParam
    organizationId: Long,
    @RequestBody @Valid dto: PromptCreateDto,
  ): PromptModel {
    val result = promptService.createPrompt(organizationId, dto)
    return promptModelAssembler.toModel(result)
  }

  @DeleteMapping("/{promptId}")
  fun deletePrompt(
    @RequestParam
    organizationId: Long,
    @RequestParam
    promptId: Long,
  ) {
    promptService.deletePrompt(organizationId, promptId)
  }

  @PostMapping("run")
  fun run(
    @Valid @RequestBody
    promptTestDto: PromptTestDto,
  ): PromptResponseDto {
    val prompt = promptService.getPrompt(promptTestDto)
    val messages = promptService.getLlmMessages(prompt, promptTestDto)

    val response = openaiApiService.translate(
      LLMParams(messages)
    )
    return PromptResponseDto(
      prompt,
      response.translated ?: "",
      response.usage,
    )
  }

  @GetMapping("get-variables")
  @Operation(summary = "Get variables")
  fun variables(
    @RequestParam projectId: Long,
    @RequestParam keyId: Long,
    @RequestParam targetLanguageId: Long,
  ): VariablesResponse {
    return VariablesResponse(promptService.getVariables(projectId, keyId, targetLanguageId))
  }
}
