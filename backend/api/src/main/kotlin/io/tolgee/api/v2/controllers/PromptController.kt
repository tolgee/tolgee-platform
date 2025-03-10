package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.prompt.PromptTestDto
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
@Tag(name = "Tags", description = "Manipulates key tags")
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
}
