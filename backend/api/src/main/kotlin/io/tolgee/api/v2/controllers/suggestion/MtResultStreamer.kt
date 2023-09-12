package io.tolgee.api.v2.controllers.suggestion

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sentry.Sentry
import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ExceptionWithMessage
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.machineTranslation.StreamedSuggestionInfo
import io.tolgee.hateoas.machineTranslation.StreamedSuggestionItem
import io.tolgee.hateoas.machineTranslation.TranslationItemModel
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.LanguageService
import io.tolgee.service.machineTranslation.MtCreditBucketService
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.machineTranslation.MtServiceInfo
import io.tolgee.service.project.ProjectService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationContext
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream
import java.io.OutputStreamWriter

class MtResultStreamer(private val dto: SuggestRequestDto, private val applicationContext: ApplicationContext) {

  private lateinit var outputStream: OutputStream

  fun stream(): StreamingResponseBody {
    init()
    return StreamingResponseBody { outputStream ->
      this.outputStream = outputStream
      writeInfo()

      if (baseBlank) {
        writer.close()
        return@StreamingResponseBody
      }

      writeServiceResultsAsync()
    }
  }

  /**
   * Init the props which setting possibly throws an exception, so it's caught by ExceptionHandler
   * before the stream is started
   */
  private fun init() {
    key = with(machineTranslationSuggestionFacade) { dto.key }
    val targetLanguage = applicationContext.getBean(LanguageService::class.java).get(dto.targetLanguageId)
    servicesToUse = mtService.getServicesToUse(targetLanguage, dto.services)
  }

  private fun writeInfo() {
    writer.writeJson(StreamedSuggestionInfo(servicesToUse.map { it.serviceType }, baseBlank))
  }

  private fun writeServiceResultsAsync() {
    runBlocking(Dispatchers.IO) {
      servicesToUse.map { it.serviceType }.map { service ->
        async {
          writeServiceResult(service)
        }
      }.awaitAll()
      writer.close()
    }
  }

  private fun writeServiceResult(service: MtServiceType) {
    try {
      with(machineTranslationSuggestionFacade) {
        catchingOutOfCredits(balanceBefore) {
          val translated = getTranslatedValue(project, key, dto, service)
          writeTranslatedValue(writer, service, translated)
        }
      }
    } catch (e: Exception) {
      writeException(e, writer, service)
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
  ): Map<MtServiceType, TranslateResult> {
    return mtService.getMachineTranslations(
      project,
      key,
      dto.baseText,
      with(machineTranslationSuggestionFacade) { dto.targetLanguage },
      setOf(service)
    )
  }

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

  private fun OutputStreamWriter.writeJson(data: Any) {
    this.write(jacksonObjectMapper().writeValueAsString(data) + "\n")
    this.flush()
  }

  private val machineTranslationSuggestionFacade by lazy {
    applicationContext.getBean(MachineTranslationSuggestionFacade::class.java)
  }

  private val mtService by lazy {
    applicationContext.getBean(MtService::class.java)
  }

  private val mtCreditBucketService by lazy {
    applicationContext.getBean(MtCreditBucketService::class.java)
  }

  private val projectHolder by lazy {
    applicationContext.getBean(ProjectHolder::class.java)
  }

  private val projectService by lazy {
    applicationContext.getBean(ProjectService::class.java)
  }

  var key: Key? = null
  private val writer by lazy { OutputStreamWriter(outputStream) }
  private lateinit var servicesToUse: Set<MtServiceInfo>

  private val balanceBefore by lazy {
    mtCreditBucketService.getCreditBalances(projectHolder.projectEntity)
  }

  private val project by lazy { projectHolder.projectEntity }
  private var baseLanguage = projectService.getOrCreateBaseLanguageOrThrow(projectHolder.project.id)

  private val baseBlank by lazy {
    mtService.getBaseTranslation(
      key = key,
      baseTranslationText = dto.baseText,
      baseLanguage = baseLanguage
    ).isNullOrBlank()
  }
}
