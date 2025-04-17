package io.tolgee.ee.service.prompt

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.HandlebarsException
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.constants.Message
import io.tolgee.dtos.LLMParams
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.ee.data.prompt.PromptVariableDto
import io.tolgee.ee.service.LLMProviderService
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Prompt
import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.model.enums.PromptVariableType
import io.tolgee.repository.PromptRepository
import io.tolgee.service.PromptService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.ImageConverter
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException
import java.io.ByteArrayInputStream

@Primary
@Service
class PromptServiceEeImpl(
  private val keyService: KeyService,
  private val projectService: ProjectService,
  private val translationService: TranslationService,
  private val fileStorage: FileStorage,
  private val screenshotService: ScreenshotService,
  private val promptRepository: PromptRepository,
  private val providerService: LLMProviderService,
  private val promptDefaultService: PromptDefaultService,
  private val promptVariablesService: PromptVariablesService,
) : PromptService {
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

  override fun findPromptOrDefaultDto(
    projectId: Long,
    promptId: Long?,
  ): PromptDto {
    if (promptId != null) {
      val prompt = promptRepository.findPrompt(projectId, promptId) ?: throw NotFoundException(Message.PROMPT_NOT_FOUND)
      return PromptDto(prompt.name, template = prompt.template, providerName = prompt.providerName)
    } else {
      return getDefaultPrompt()
    }
  }

  override fun findPrompt(
    projectId: Long,
    promptId: Long,
  ): Prompt {
    return promptRepository.findPrompt(projectId, promptId) ?: throw NotFoundException(Message.PROMPT_NOT_FOUND)
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

  @Transactional
  fun getPrompt(
    projectId: Long,
    data: PromptRunDto,
  ): String {
    try {
      val params = promptVariablesService.getVariables(projectId, data.keyId, data.targetLanguageId)

      val handlebars = Handlebars()

      val paramsForFragments = createVariablesLazyMap(params)

      val fragments = params.find { it.name == "fragment" }?.props

      fragments?.forEach {
        val template = handlebars.compileInline(it.value)
        it.value = template.apply(paramsForFragments)
      }

      val finalParams = createVariablesLazyMap(params)

      val template = handlebars.compileInline(data.template)
      val prompt = template.apply(finalParams)
      // remove excessive newlines and trim
      return prompt.replace(Regex("\n(\\s*\n)+"), "\n\n").trim()
    } catch (e: HandlebarsException) {
      throw BadRequestException(
        Message.LLM_TEMPLATE_PARSING_ERROR,
        listOf(e.error.reason, e.error.line, e.error.column),
      )
    }
  }

  fun createVariablesLazyMap(params: List<Variable>): LazyMap {
    val mapParams =
      params.map {
        it.name to it
      }.toMap()
    val lazyMap = LazyMap()
    lazyMap.setMap(mapParams)
    return lazyMap
  }

  @Transactional
  fun getLLMParamsFromPrompt(
    prompt: String,
    keyId: Long,
  ): LLMParams {
    val key = keyService.find(keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    var preparedPrompt = prompt

    val pattern = Regex("\\[\\[screenshot_(full|small)_(\\d+)]]")

    val shouldOutputJson = preparedPrompt.contains(LLM_MARK_JSON)
    if (!shouldOutputJson) {
      preparedPrompt = preparedPrompt.replace(LLM_MARK_JSON, "")
    }

    val parts = pattern.splitWithMatches(preparedPrompt)
    val messages =
      parts.mapNotNull {
        if (pattern.matches(it)) {
          val match = pattern.matchEntire(it) ?: throw Error()
          // Extract size and id from the match groups
          val size = match.groups[1]!!.value // full or small
          val id = match.groups[2]!!.value.toLong() // number
          val screenshot = key.keyScreenshotReferences.find { it.screenshot.id == id }?.screenshot
          if (screenshot == null) {
            null
          } else {
            val file =
              if (size == "full") {
                screenshot.filename
              } else {
                screenshot.middleSizedFilename ?: screenshot.filename
              }

            val filePath = screenshotService.getScreenshotPath(file)

            lateinit var image: ByteArray

            if (screenshot.keyScreenshotReferences.find { it.key.id == key.id } !== null) {
              val converter =
                ImageConverter(
                  ByteArrayInputStream(
                    fileStorage.readFile(filePath),
                  ),
                )
              image = converter.highlightKeys(screenshot, listOf(key.id)).toByteArray()
            } else {
              image = fileStorage.readFile(filePath)
            }

            LLMParams.Companion.LlmMessage(
              type = LLMParams.Companion.LlmMessageType.IMAGE,
              image = image,
            )
          }
        } else {
          LLMParams.Companion.LlmMessage(
            type = LLMParams.Companion.LlmMessageType.TEXT,
            text = it,
          )
        }
      }
    return LLMParams(messages, shouldOutputJson)
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
    organizationId: Long,
    params: LLMParams,
    provider: String,
    priority: LLMProviderPriority?,
  ): PromptService.Companion.PromptResult {
    val result =
      try {
        providerService.callProvider(organizationId, provider, params, priority)
      } catch (e: RestClientException) {
        throw BadRequestException(Message.LLM_PROVIDER_ERROR, listOf(e.message))
      } catch (e: TranslationApiRateLimitException) {
        throw BadRequestException(Message.LLM_PROVIDER_ERROR, listOf(e.message))
      }

    result.parsedJson =
      try {
        jacksonObjectMapper().readValue<JsonNode>(result.response)
      } catch (e: JsonProcessingException) {
        null
      }

    return result
  }

  fun getTranslationFromPromptResult(result: PromptService.Companion.PromptResult): MtValueProvider.MtResult {
    val json = result.parsedJson ?: throw BadRequestException(Message.LLM_PROVIDER_NOT_RETURNED_JSON)
    val translation = json.get("output").asText() ?: throw BadRequestException(Message.LLM_PROVIDER_NOT_RETURNED_JSON)

    return MtValueProvider.MtResult(
      translation,
      contextDescription = json.get("contextDescription").asText(),
      price = result.price,
      usage = result.usage,
    )
  }

  @Transactional
  override fun translate(
    projectId: Long,
    data: PromptRunDto,
    priority: LLMProviderPriority?,
  ): MtValueProvider.MtResult {
    val project = projectService.get(projectId)
    val prompt = getPrompt(projectId, data)
    val params = getLLMParamsFromPrompt(prompt, data.keyId)
    val result = runPrompt(project.organizationOwner.id, params, data.provider, priority)
    return getTranslationFromPromptResult(result)
  }

  override fun translateAndUpdateTranslation(
    projectId: Long,
    data: PromptRunDto,
    priority: LLMProviderPriority?,
  ) {
    val result = translate(projectId, data, priority)
    val translation = translationService.getOrCreate(data.keyId, data.targetLanguageId)
    translation.text = result.translated
    translationService.save(translation)
  }

  fun getDefaultPrompt(): PromptDto {
    return promptDefaultService.getDefaultPrompt()
  }

  companion object {
    class LazyMap : AbstractMap<String, Any?>() {
      private lateinit var internalMap: Map<String, Variable>

      fun setMap(map: Map<String, Variable>) {
        internalMap = map
      }

      override fun get(key: String): Any? {
        val promptValue = internalMap.get(key)

        if (!promptValue?.props.isNullOrEmpty()) {
          val mapParams =
            promptValue!!.props.map {
              it.name to it
            }.toMap()

          val lazyMap = LazyMap()
          lazyMap.setMap(mapParams)
          return lazyMap
        }

        val stringValue = promptValue?.lazyValue?.let { it() } ?: promptValue?.value
        return stringValue?.let { Handlebars.SafeString(it) }
      }

      override val entries: Set<Map.Entry<String, Any?>>
        get() {
          return internalMap.entries.map { (key) -> Entry(key) { get(key) } }.toSet()
        }
    }

    private class Entry(override val key: String, val valGetter: () -> Any?) : Map.Entry<String, Any?> {
      override val value: Any?
        get() = valGetter()
    }

    class Variable(
      val name: String,
      var value: String? = null,
      var lazyValue: (() -> String?)? = null,
      val description: String? = null,
      val props: MutableList<Variable> = mutableListOf(),
      val type: PromptVariableType? = null,
    ) {
      fun toPromptVariableDto(): PromptVariableDto {
        val computedType =
          if (props.isNotEmpty()) {
            PromptVariableType.OBJECT
          } else {
            type ?: PromptVariableType.STRING
          }

        return PromptVariableDto(
          name = name,
          description = description,
          value = value,
          props =
            if (props.isNotEmpty()) {
              props.map { it.toPromptVariableDto() }.toMutableList()
            } else {
              null
            },
          type = computedType,
        )
      }
    }
  }
}
