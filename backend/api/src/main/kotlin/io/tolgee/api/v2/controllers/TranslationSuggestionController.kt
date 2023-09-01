package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sentry.Sentry
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.invitation.TranslationMemoryItemModelAssembler
import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.MtCreditBalanceDto
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ExceptionWithMessage
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.hateoas.machineTranslation.StreamedSuggestionInfo
import io.tolgee.hateoas.machineTranslation.StreamedSuggestionItem
import io.tolgee.hateoas.machineTranslation.SuggestResultModel
import io.tolgee.hateoas.machineTranslation.TranslationItemModel
import io.tolgee.hateoas.translationMemory.TranslationMemoryItemModel
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.LanguageService
import io.tolgee.service.key.KeyService
import io.tolgee.service.machineTranslation.MtCreditBucketService
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationMemoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStreamWriter
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/suggest", "/v2/projects/suggest"])
@Tag(name = "Translation suggestion")
@Suppress("SpringJavaInjectionPointsAutowiringInspection", "MVCPathVariableInspection")
class TranslationSuggestionController(
  private val projectHolder: ProjectHolder,
  private val languageService: LanguageService,
  private val keyService: KeyService,
  private val mtService: MtService,
  private val mtCreditBucketService: MtCreditBucketService,
  private val translationMemoryService: TranslationMemoryService,
  private val translationMemoryItemModelAssembler: TranslationMemoryItemModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val arraytranslationMemoryItemModelAssembler: PagedResourcesAssembler<TranslationMemoryItemView>,
  private val securityService: SecurityService
) {
  @PostMapping("/machine-translations")
  @Operation(summary = "Suggests machine translations from enabled services")
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.TRANSLATIONS_EDIT)
  fun suggestMachineTranslations(@RequestBody @Valid dto: SuggestRequestDto): SuggestResultModel {
    val targetLanguage = dto.targetLanguage

    securityService.checkLanguageTranslatePermission(projectHolder.project.id, listOf(targetLanguage.id))

    val balanceBefore = mtCreditBucketService.getCreditBalances(projectHolder.projectEntity)

    return catchingOutOfCredits(balanceBefore) {
      val resultData = getTranslationResults(dto, targetLanguage)
      val balanceAfter = mtCreditBucketService.getCreditBalances(projectHolder.projectEntity)

      SuggestResultModel(
        machineTranslations = resultData?.map { it.key to it.value.output }?.toMap(),
        result = resultData,
        translationCreditsBalanceBefore = balanceBefore.creditBalance / 100,
        translationCreditsBalanceAfter = balanceAfter.creditBalance / 100,
        translationExtraCreditsBalanceBefore = balanceBefore.extraCreditBalance / 100,
        translationExtraCreditsBalanceAfter = balanceAfter.extraCreditBalance / 100,
      )
    }
  }

  @PostMapping("/translation-memory")
  @Operation(
    summary = "Suggests machine translations from translation memory." +
      "\n\nThe result is always sorted by similarity, so sorting is not supported."
  )
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.TRANSLATIONS_EDIT)
  fun suggestTranslationMemory(
    @RequestBody @Valid dto: SuggestRequestDto,
    @ParameterObject pageable: Pageable
  ): PagedModel<TranslationMemoryItemModel> {
    val targetLanguage = languageService.findById(dto.targetLanguageId)
      .orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }

    securityService.checkLanguageTranslatePermission(projectHolder.project.id, listOf(targetLanguage.id))

    val data = dto.baseText?.let { baseText -> translationMemoryService.suggest(baseText, targetLanguage, pageable) }
      ?: let {
        val key = keyService.findOptional(dto.keyId).orElseThrow { NotFoundException(Message.KEY_NOT_FOUND) }
        key.checkInProject()
        translationMemoryService.suggest(key, targetLanguage, pageable)
      }
    return arraytranslationMemoryItemModelAssembler.toModel(data, translationMemoryItemModelAssembler)
  }

  @PostMapping("/machine-translations-streaming", produces = ["application/x-ndjson"])
  @Operation(
    summary = "Suggests machine translations from enabled services (streaming).\n" +
      "If an error occurs when any of the services is used," +
      " the error information is returned as a part of the result item, while the response has 200 status code."
  )
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.TRANSLATIONS_EDIT)
  fun suggestMachineTranslationsStreaming(@RequestBody @Valid dto: SuggestRequestDto): StreamingResponseBody {
    val key = dto.key
    val servicesToUse = mtService.getServicesToUse(dto.targetLanguageId, dto.services)
    val balanceBefore = mtCreditBucketService.getCreditBalances(projectHolder.projectEntity)
    val project = projectHolder.projectEntity

    return StreamingResponseBody { outputStream ->
      val writer = OutputStreamWriter(outputStream)
      writer.writeJson(StreamedSuggestionInfo(servicesToUse.map { it.serviceType }))
      runBlocking(Dispatchers.IO) {
        servicesToUse.map { it.serviceType }.map { service ->
          async {
            try {
              catchingOutOfCredits(balanceBefore) {
                val translated = getTranslatedValue(project, key, dto, service)
                writeTranslatedValue(writer, service, translated)
              }
            } catch (e: Exception) {
              writeException(e, writer, service)
            }
          }
        }.awaitAll()
        writer.close()
      }
    }
  }

  private fun Key.checkInProject() {
    keyService.checkInProject(this, projectHolder.project.id)
  }

  private fun getTranslationResults(
    dto: SuggestRequestDto,
    targetLanguage: Language
  ): Map<MtServiceType, TranslationItemModel>? {
    val key = dto.key

    val resultMap =
      mtService.getMachineTranslations(projectHolder.projectEntity, key, dto.baseText, targetLanguage, dto.services)

    val resultData = resultMap
      ?.map { (key, value) ->
        key to TranslationItemModel(value?.translatedText.orEmpty(), value?.contextDescription)
      }?.toMap()
    return resultData
  }

  private fun <T> catchingOutOfCredits(balanceBefore: MtCreditBalanceDto, fn: () -> T): T {
    try {
      return fn()
    } catch (e: OutOfCreditsException) {
      if (e.reason == OutOfCreditsException.Reason.SPENDING_LIMIT_EXCEEDED) {
        throw BadRequestException(
          Message.CREDIT_SPENDING_LIMIT_EXCEEDED,
        )
      }
      throw BadRequestException(
        Message.OUT_OF_CREDITS,
        listOf(balanceBefore.creditBalance, balanceBefore.extraCreditBalance)
      )
    }
  }

  private val SuggestRequestDto.key
    get() = if (this.baseText != null) {
      null
    } else {
      keyService.findOptional(this.keyId).orElseThrow { NotFoundException(Message.KEY_NOT_FOUND) }?.also {
        it.checkInProject()
      }
    }

  private val SuggestRequestDto.targetLanguage
    get() = languageService.findById(this.targetLanguageId)
      .orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }

  private fun writeException(
    e: Exception,
    writer: OutputStreamWriter,
    service: MtServiceType
  ) {
    val exceptionWithMessage = (e as? ExceptionWithMessage)
    writer.writeJson(
      StreamedSuggestionItem(
        service,
        null,
        errorMessage = exceptionWithMessage?.tolgeeMessage,
        errorParams = exceptionWithMessage?.params,
        errorException = e::class.qualifiedName
      )
    )
    if (e !is BadRequestException && e !is NotFoundException) {
      Sentry.captureException(e)
    }
  }

  private fun writeTranslatedValue(
    writer: OutputStreamWriter,
    service: MtServiceType,
    translated: Map<MtServiceType, TranslateResult?>?
  ) {
    val model = translated?.get(service)
      ?.let { it.translatedText?.let { text -> TranslationItemModel(text, it.contextDescription) } }
    writer.writeJson(
      StreamedSuggestionItem(
        service,
        model

      )
    )
  }

  private fun getTranslatedValue(
    project: Project,
    key: Key?,
    dto: SuggestRequestDto,
    service: MtServiceType
  ): Map<MtServiceType, TranslateResult?>? {
    return mtService.getMachineTranslations(
      project,
      key,
      dto.baseText,
      dto.targetLanguage,
      setOf(service)
    )
  }

  private fun OutputStreamWriter.writeJson(data: Any) {
    this.write(jacksonObjectMapper().writeValueAsString(data) + "\n")
    this.flush()
  }
}
