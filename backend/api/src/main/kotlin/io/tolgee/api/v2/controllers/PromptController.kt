package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.machineTranslation.providers.tolgee.LLMParams
import io.tolgee.component.machineTranslation.providers.tolgee.OpenaiApiService
import io.tolgee.dtos.request.prompt.PromptTestDto
import io.tolgee.dtos.request.prompt.VariablesResponse
import io.tolgee.dtos.response.PromptResponseDto
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.service.PromptService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/prompts/",
  ],
)
@OpenApiOrderExtension(6)
class PromptController(
  private val promptService: PromptService,
  private val openaiApiService: OpenaiApiService,
  private val keyService: KeyService,
  private val fileStorage: FileStorage,
  private val screenshotService: ScreenshotService
) {
  @OptIn(ExperimentalEncodingApi::class)
  @PostMapping("test")
  fun test(
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
      response.translated ?: ""
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
