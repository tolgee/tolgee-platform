package io.tolgee.ee.service.prompt

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.HandlebarsException
import io.tolgee.ee.component.PromptLazyMap
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.PromptResult
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.FailedDependencyException
import io.tolgee.exceptions.LlmProviderNotReturnedJsonException
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.TooManyRequestsException
import io.tolgee.model.Prompt
import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.repository.PromptRepository
import io.tolgee.service.PromptService
import io.tolgee.service.key.KeyService
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.service.project.ProjectService
import io.tolgee.util.updateStringsInJson
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException

@Primary
@Service
class PromptServiceEeImpl(
  private val keyService: KeyService,
  private val projectService: ProjectService,
  private val promptRepository: PromptRepository,
  private val providerService: LlmProviderService,
  private val defaultPromptHelper: DefaultPromptHelper,
  private val promptVariablesHelper: PromptVariablesHelper,
  @Lazy
  private val mtServiceConfigService: MtServiceConfigService,
  private val applicationContext: ApplicationContext,
  private val promptParamsHelper: PromptParamsHelper
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
        basicPromptOptions = dto.basicPromptOptions?.toTypedArray(),
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
      return PromptDto(
        prompt.name,
        template = prompt.template,
        providerName = prompt.providerName,
        basicPromptOptions = prompt.basicPromptOptions?.toList(),
      )
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

  override fun deleteAllByProjectId(projectId: Long) {
    return promptRepository.deleteAllByProjectId(projectId)
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
    prompt.basicPromptOptions = dto.basicPromptOptions?.toTypedArray()
    promptRepository.save(prompt)
    return prompt
  }

  fun deletePrompt(
    projectId: Long,
    promptId: Long,
  ) {
    val prompt = this.findPrompt(projectId, promptId)
    val project = projectService.get(projectId)
    mtServiceConfigService.removePrompt(project, prompt.id)
    promptRepository.delete(prompt)
  }

  @Transactional
  fun getPrompt(
    projectId: Long,
    template: String,
    keyId: Long?,
    targetLanguageId: Long,
    options: List<BasicPromptOption>?,
  ): String {
    try {
      val params = promptVariablesHelper.getVariables(projectId, keyId, targetLanguageId)

      val handlebars = Handlebars()

      val paramsForFragments = createVariablesLazyMap(params)

      val fragments = params.find { it.name == "fragment" }?.props

      fragments?.forEach {
        val included = it.option == null || (options?.contains(it.option) ?: true)
        if (included && it.value is String) {
          val renderedTemplate = handlebars.compileInline(it.value as String)
          it.value = renderedTemplate.apply(paramsForFragments)
        } else {
          it.value = ""
        }
      }

      val finalParams = createVariablesLazyMap(params)

      val renderedTemplate = handlebars.compileInline(template)
      val prompt = renderedTemplate.apply(finalParams)
      // remove excessive newlines and trim
      return prompt.replace(Regex("\n(\\s*\n)+"), "\n\n").trim()
    } catch (e: HandlebarsException) {
      throw BadRequestException(
        Message.LLM_TEMPLATE_PARSING_ERROR,
        listOf(e.error.reason, e.error.line, e.error.column),
      )
    }
  }

  fun createVariablesLazyMap(params: List<PromptLazyMap.Companion.Variable>): PromptLazyMap {
    val mapParams =
      params.associateBy { it.name }
    val lazyMap = PromptLazyMap()
    lazyMap.setMap(mapParams)
    return lazyMap
  }

  @Transactional
  fun getLlmParamsFromPrompt(
    prompt: String,
    keyId: Long?,
    priority: LlmProviderPriority,
  ): LlmParams {
    val key = keyId?.let {
      keyService.find(it)
      ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    }
    return promptParamsHelper.getParamsFromPrompt(prompt, key, priority)
  }

  fun runPromptWithoutChargingCredits(
    organizationId: Long,
    params: LlmParams,
    provider: String,
    attempts: List<Int>? = null,
  ): PromptResult {
    val result =
      try {
        providerService.callProvider(organizationId, provider, params, attempts)
      } catch (e: RestClientException) {
        throw FailedDependencyException(Message.LLM_PROVIDER_ERROR, listOf(e.message), e)
      }

    result.parsedJson = extractJsonFromResponse(result.response)
    return result
  }

  fun getJsonLike(content: String): String {
    return "{${content.substringAfter("{").substringBeforeLast("}")}}"
  }

  fun extractJsonFromResponse(content: String): JsonNode? {
    // attempting different strategies to find a json in the response
    val attempts = listOf<(String) -> String>(
      { it },
      { getJsonLike(it) },
      { getJsonLike(it.substringAfter("```").substringBefore("```")) },
    )
    for (attempt in attempts) {
      val result = parseJsonSafely(attempt.invoke(content))
      if (result != null) {
        return result
      }
    }
    return null
  }

  fun parseJsonSafely(content: String): JsonNode? {
    return try {
      val result = jacksonObjectMapper().readValue<JsonNode>(content)
      updateStringsInJson(result) {
          // gpt-4.1 sometimes includes NIL,
          // which is invalid utf-8 character breaking DB saving
          it.replace("\u0000", "")
      }
    } catch (_: JsonProcessingException) {
      null
    }
  }

  fun runPromptAndChargeCredits(
    organizationId: Long,
    params: LlmParams,
    provider: String,
    attempts: List<Int>? = null,
  ): PromptResult {
    publishBeforeEvent(organizationId)
    val result = runPromptWithoutChargingCredits(organizationId, params, provider, attempts)
    publishAfterEvent(organizationId, result.price)
    return result
  }

  fun <T> rateLimitToStatus(func: () -> T): T {
    return try {
      func()
    } catch (e: LlmRateLimitedException) {
      throw TooManyRequestsException(Message.TOO_MANY_REQUESTS, cause = e)
    }
  }

  fun getTranslationFromPromptResult(result: PromptResult): MtValueProvider.MtResult {
    val json = result.parsedJson ?: throw LlmProviderNotReturnedJsonException()
    val translation = json.get("output")?.asText() ?: throw LlmProviderNotReturnedJsonException()

    return MtValueProvider.MtResult(
      translation,
      contextDescription = json.get("contextDescription")?.asText(),
      price = result.price,
      usage = result.usage,
    )
  }

  @Transactional
  override fun translate(
    projectId: Long,
    data: PromptRunDto,
    priority: LlmProviderPriority?,
  ): MtValueProvider.MtResult {
    val project = projectService.get(projectId)
    val prompt =
      getPrompt(
        projectId,
        data.template ?: defaultPromptHelper.getDefaultPrompt().template!!,
        data.keyId,
        data.targetLanguageId,
        data.basicPromptOptions,
      )
    val params = getLlmParamsFromPrompt(prompt, data.keyId, priority ?: LlmProviderPriority.HIGH)
    val result = runPromptWithoutChargingCredits(project.organizationOwner.id, params, data.provider)
    return getTranslationFromPromptResult(result)
  }

  fun getDefaultPrompt(): PromptDto {
    return defaultPromptHelper.getDefaultPrompt()
  }

  private fun publishBeforeEvent(organizationId: Long) {
    applicationContext.publishEvent(
      OnBeforeMachineTranslationEvent(this, organizationId),
    )
  }

  private fun publishAfterEvent(
    organizationId: Long,
    actualPriceInCents: Int,
  ) {
    applicationContext.publishEvent(
      OnAfterMachineTranslationEvent(this, organizationId, actualPriceInCents),
    )
  }
}
