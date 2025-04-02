package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.component.machineTranslation.providers.llm.LLMParams
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.dtos.request.prompt.VariablesResponseDto
import io.tolgee.dtos.response.PromptResponseDto
import io.tolgee.ee.service.PromptServiceEeImpl
import io.tolgee.hateoas.prompt.PromptModel
import io.tolgee.ee.api.v2.hateoas.assemblers.PromptModelAssembler
import io.tolgee.model.Prompt
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authorization.UseDefaultPermissions
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
    @RequestBody @Valid dto: PromptDto,
  ): PromptModel {
    val result = promptService.createPrompt(projectHolder.project.id, dto)
    return promptModelAssembler.toModel(result)
  }

  @PutMapping("/{promptId}")
  @UseDefaultPermissions
  fun updatePrompt(
    @PathVariable promptId: Long,
    @RequestBody @Valid dto: PromptDto,
  ): PromptModel {
    val result = promptService.updatePrompt(projectHolder.project.id, promptId, dto)
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
    @Valid @RequestBody promptRunDto: PromptRunDto,
  ): PromptResponseDto {
    val prompt = promptService.getPrompt(projectHolder.project.id, promptRunDto)
    val messages = promptService.getLlmMessages(prompt, promptRunDto)
    val response = promptService.runPrompt(
      projectHolder.project.organizationOwnerId,
      LLMParams(messages),
      promptRunDto
    )
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
    @RequestParam keyId: Long?,
    @RequestParam targetLanguageId: Long?,
  ): VariablesResponseDto {
    return VariablesResponseDto(
      promptService.getVariables(projectHolder.project.id, keyId, targetLanguageId)
        .map { it.toPromptVariableDto() }.toMutableList()
    )
  }
}
