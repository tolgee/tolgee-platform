package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.dtos.request.prompt.PromptTestDto
import io.tolgee.dtos.request.prompt.VariablesResponse
import io.tolgee.dtos.response.PromptResponseDto
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.service.PromptService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/prompts/",
  ],
)
@OpenApiOrderExtension(6)
class PromptController(private val promptService: PromptService) {
  @PostMapping("test")
  fun test(
    @Valid @RequestBody
    promptTestDto: PromptTestDto,
  ): PromptResponseDto {
    val prompt = promptService.getPrompt(promptTestDto)
    return PromptResponseDto(prompt)
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
