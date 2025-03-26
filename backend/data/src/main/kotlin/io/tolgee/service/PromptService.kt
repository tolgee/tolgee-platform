package io.tolgee.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jknack.handlebars.Handlebars
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.llm.LLMParams
import io.tolgee.component.machineTranslation.providers.llm.OllamaApiService
import io.tolgee.component.machineTranslation.providers.llm.OpenaiApiService
import io.tolgee.constants.Message
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptTestDto
import io.tolgee.dtos.request.prompt.PromptVariable
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LLMProviderType
import io.tolgee.repository.PromptRepository
import io.tolgee.security.ProjectHolder
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.MetadataKey
import io.tolgee.service.machineTranslation.MetadataProvider
import io.tolgee.service.machineTranslation.MtTranslatorContext
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.ImageConverter
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import java.io.ByteArrayInputStream
import kotlin.collections.HashMap
import kotlin.jvm.optionals.getOrNull

@Service
class PromptService(
  private val securityService: SecurityService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val projectService: ProjectService,
  private val translationService: TranslationService,
  private val fileStorage: FileStorage,
  private val screenshotService: ScreenshotService,
  private val applicationContext: ApplicationContext,
  private val promptRepository: PromptRepository,
  private val providerService: LLMProviderService,
  private val openaiApiService: OpenaiApiService,
  private val ollamaApiService: OllamaApiService,
  private val projectHolder: ProjectHolder,
) {
  fun getAllPaged(
    projectId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<Prompt> {
    return promptRepository.getAllPaged(projectId, pageable, search)
  }

  fun createPrompt(
    projectId: Long,
    dto: PromptDto,
  ): Prompt {
    val prompt =
      Prompt(
        name = dto.name,
        template = dto.template,
        project = projectService.get(projectId),
        providerName = dto.providerName,
      )
    promptRepository.save(prompt)
    return prompt
  }

  fun findPrompt(
    projectId: Long,
    promtId: Long,
  ): Prompt {
    return promptRepository.findPrompt(projectId, promtId) ?: throw NotFoundException(Message.PROMPT_NOT_FOUND)
  }

  fun updatePrompt(
    projectId: Long,
    promptId: Long,
    dto: PromptDto,
  ): Prompt {
    val prompt = findPrompt(projectId, promptId)
    prompt.name = dto.name
    prompt.template = dto.template
    prompt.providerName = dto.providerName
    promptRepository.save(prompt)
    return prompt
  }

  fun deletePrompt(
    projectId: Long,
    promptId: Long,
  ) {
    val prompt = this.findPrompt(projectId, promptId)
    promptRepository.delete(prompt)
  }

  fun encodeScreenshot(
    number: Long,
    type: String,
  ): String {
    return "[[screenshot_${type}_$number]]"
  }

  fun encodeScreenshots(
    list: List<Any>,
    type: String,
  ): String {
    return list
      .mapIndexed { index, _ -> encodeScreenshot(index.toLong(), type) }
      .joinToString("\n")
  }

  fun getVariables(
    projectId: Long,
    keyId: Long,
    targetLanguageId: Long,
  ): MutableList<PromptVariable> {
    securityService.checkLanguageViewPermission(
      projectId,
      listOf(targetLanguageId),
    )
    val key = keyService.find(keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    keyService.checkInProject(key, projectId)
    val project = projectService.get(projectId)

    val tLanguage =
      languageService.find(targetLanguageId, projectId) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
    val sLanguage = languageService.get(project.baseLanguage!!.id, projectId)

    val sTranslation = translationService.find(key, sLanguage).getOrNull()
    val tTranslation = translationService.find(key, tLanguage).getOrNull()

    val variables = mutableListOf<PromptVariable>()

    variables.add(PromptVariable("source", sLanguage.name))
    variables.add(PromptVariable("target", tLanguage.name))

    variables.add(PromptVariable("projectName", project.name))
    variables.add(PromptVariable("projectDescription", project.aiTranslatorPromptDescription ?: ""))
    variables.add(PromptVariable("sourceText", sTranslation?.text ?: ""))
    variables.add(PromptVariable("targetText", tTranslation?.text ?: ""))
    variables.add(PromptVariable("keyName", key.name))

    val screenshots = key.keyScreenshotReferences

    variables.add(
      PromptVariable(
        "allScreenshots",
        encodeScreenshots(screenshots, "small"),
      ),
    )

    variables.add(
      PromptVariable(
        "allScreenshotsFull",
        encodeScreenshots(screenshots, "full"),
      ),
    )

    variables.add(
      PromptVariable(
        "firstScreenshot",
        encodeScreenshots(screenshots.take(1), "small"),
      ),
    )

    variables.add(
      PromptVariable(
        "firstScreenshotFull",
        encodeScreenshots(screenshots.take(1), "full"),
      ),
    )

    variables.add(
      PromptVariable(
        "relatedKeysJson",
        "Related keys in json format (based on context extraction)",
        lazyValue = {
          val context = MtTranslatorContext(projectId, applicationContext, false)
          val metadataProvider = MetadataProvider(context)
          val closeItems =
            metadataProvider.getCloseItems(
              sLanguage,
              tLanguage,
              MetadataKey(key.id, sTranslation?.text ?: "", tLanguage.id),
            )
          closeItems.joinToString("\n") {
            val mapper = ObjectMapper()
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            mapper.writeValueAsString(it)
          }
        },
      ),
    )

    variables.add(
      PromptVariable(
        "fragmentReturnJson",
        """Return result in json
```
{
   "output": <translation>,
   "contextDescription": <description>
}
```""",
      ),
    )

    return variables
  }

  fun getPrompt(
    projectId: Long,
    data: PromptTestDto,
  ): String {
    val params = getVariables(projectId, data.keyId, data.targetLanguageId)
    val handlebars = Handlebars()

    val mapParams =
      params.map {
        it.name to it
      }.toMap()

    val lazyMap = LazyMap()
    lazyMap.setMap(mapParams)

    val template = handlebars.compileInline(data.template)
    val prompt = template.apply(lazyMap)
    return prompt
  }

  fun getLlmMessages(
    prompt: String,
    data: PromptTestDto,
  ): List<LLMParams.Companion.LlmMessage> {
    val key = keyService.find(data.keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)

    val pattern = Regex("\\[\\[screenshot_(full|small)_(\\d+)]]")

    val parts = pattern.splitWithMatches(prompt)
    return parts.map {
      if (pattern.matches(it)) {
        val match = pattern.matchEntire(it) ?: throw Error()
        // Extract size and number from the match groups
        val size = match.groups[1]!!.value // full or small
        val number = match.groups[2]!!.value.toInt() // number
        val screenshot = key.keyScreenshotReferences[number].screenshot
        val file =
          if (size === "full") {
            screenshot.filename
          } else {
            screenshot.middleSizedFilename ?: screenshot.filename
          }

        var image =
          fileStorage.readFile(
            screenshotService.getScreenshotPath(file),
          )

        if (screenshot.keyScreenshotReferences.find { it.key.id == key.id } !== null) {
          val converter =
            ImageConverter(
              ByteArrayInputStream(
                fileStorage.readFile(
                  screenshotService.getScreenshotPath(file),
                ),
              ),
            )
          image = converter.highlightKeys(screenshot, listOf(key.id)).toByteArray()
        }

        LLMParams.Companion.LlmMessage(
          type = LLMParams.Companion.LlmMessageType.IMAGE,
          image = image,
        )
      } else {
        LLMParams.Companion.LlmMessage(
          type = LLMParams.Companion.LlmMessageType.TEXT,
          text = it,
        )
      }
    }
  }

  // Helper function to split and keep matches
  fun Regex.splitWithMatches(input: String): List<String> {
    val result = mutableListOf<String>()
    var lastIndex = 0

    this.findAll(input).forEach { match ->
      // Add text before the match if exists
      if (match.range.first > lastIndex) {
        result.add(input.substring(lastIndex, match.range.first))
      }
      // Add the match itself
      result.add(match.value)
      lastIndex = match.range.last + 1
    }

    // Add remaining text after last match
    if (lastIndex < input.length) {
      result.add(input.substring(lastIndex))
    }

    return result
  }

  fun runPrompt(
    params: LLMParams,
    promptTestDto: PromptTestDto,
  ): MtValueProvider.MtResult {
    val providerConfig =
      providerService.getProviderByName(projectHolder.project.organizationOwnerId, promptTestDto.provider)
        ?: throw BadRequestException(Message.LLM_PROVIDER_NOT_FOUND, listOf(promptTestDto.provider))

    try {
      return when (providerConfig.type) {
        LLMProviderType.OPENAI -> openaiApiService.translate(params, providerConfig)
        LLMProviderType.OLLAMA -> ollamaApiService.translate(params, providerConfig)
      }
    } catch (e: ResourceAccessException) {
      throw BadRequestException(Message.LLM_PROVIDER_ERROR, listOf(e.message))
    } catch (e: HttpClientErrorException) {
      throw BadRequestException(Message.LLM_PROVIDER_ERROR, listOf(e.message))
    }
  }

  companion object {
    class LazyMap : HashMap<String, Handlebars.SafeString>() {
      private lateinit var internalMap: Map<String, PromptVariable>

      fun setMap(map: Map<String, PromptVariable>) {
        internalMap = map
      }

      override fun get(key: String): Handlebars.SafeString? {
        val promptValue = internalMap.get(key)
        val stringValue = promptValue?.lazyValue?.let { it() } ?: promptValue?.value
        return stringValue?.let { Handlebars.SafeString(it) }
      }
    }
  }
}
