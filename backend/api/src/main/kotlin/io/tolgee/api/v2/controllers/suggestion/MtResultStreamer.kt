package io.tolgee.api.v2.controllers.suggestion

import com.fasterxml.jackson.databind.ObjectMapper
import io.sentry.Sentry
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ExceptionWithMessage
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.machineTranslation.StreamedSuggestionInfo
import io.tolgee.hateoas.machineTranslation.StreamedSuggestionItem
import io.tolgee.hateoas.machineTranslation.TranslationItemModel
import io.tolgee.security.ProjectHolder
import io.tolgee.service.machineTranslation.MachineTranslationParams
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.machineTranslation.MtTranslatorResult
import io.tolgee.util.Logging
import io.tolgee.util.StreamingResponseBodyProvider
import io.tolgee.util.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationContext
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream
import java.io.OutputStreamWriter

class MtResultStreamer(
  private val dto: SuggestRequestDto,
  private val applicationContext: ApplicationContext,
  private val streamingResponseBodyProvider: StreamingResponseBodyProvider,
) : Logging {
  private lateinit var outputStream: OutputStream
  private lateinit var project: ProjectDto

  fun stream(): StreamingResponseBody {
    project = projectHolder.project
    val info = getInfo()

    return streamingResponseBodyProvider.createStreamingResponseBody { outputStream ->
      this.outputStream = outputStream
      writer.writeJson(info)
      if (!baseBlank) {
        writeServiceResultsAsync()
      }

      writer.close()
    }
  }

  private fun getInfo(): StreamedSuggestionInfo {
    return StreamedSuggestionInfo(servicesToUse.map { it.serviceType }, baseBlank)
  }

  private fun writeServiceResultsAsync() {
    runBlocking(Dispatchers.IO) {
      servicesToUse.map { it.serviceType }.map { service ->
        async {
          writeServiceResult(service)
        }
      }.awaitAll()
    }
  }

  private val servicesToUse
    get() = mtTranslator.getServicesToUseByDesiredServices(dto.targetLanguageId, dto.services)

  private fun writeServiceResult(service: MtServiceType) {
    try {
      with(machineTranslationSuggestionFacade) {
        catchingOutOfCredits(project.organizationOwnerId) {
          val translated = getTranslatedValue(dto, service)
          translated?.exception?.let { throw it }
          writeTranslatedValue(writer, service, translated)
        }
      }
    } catch (e: Exception) {
      logger.debug("Error while streaming machine translation suggestion", e)
      writeException(e, writer, service)
      if (e !is BadRequestException && e !is NotFoundException) {
        Sentry.captureException(e)
        logger.error("Error while streaming machine translation suggestion", e)
      }
    }
  }

  private fun writeTranslatedValue(
    writer: OutputStreamWriter,
    service: MtServiceType,
    translated: MtTranslatorResult?,
  ) {
    val model =
      translated
        ?.let { it.translatedText?.let { text -> TranslationItemModel(text, it.contextDescription) } }

    writer.writeJson(
      StreamedSuggestionItem(
        service,
        model,
      ),
    )
  }

  private fun getTranslatedValue(
    dto: SuggestRequestDto,
    service: MtServiceType,
  ): MtTranslatorResult? {
    return mtTranslator.translate(
      listOf(
        MachineTranslationParams(
          keyId = dto.keyId,
          baseTranslationText = dto.baseText,
          targetLanguageId = dto.targetLanguageId,
          desiredServices = setOf(service),
        ),
      ),
    ).singleOrNull()
  }

  private fun writeException(
    e: Exception,
    writer: OutputStreamWriter,
    service: MtServiceType,
  ) {
    val exceptionWithMessage = (e as? ExceptionWithMessage)
    writer.writeJson(
      StreamedSuggestionItem(
        service,
        null,
        errorMessage = exceptionWithMessage?.tolgeeMessage,
        errorParams = exceptionWithMessage?.params,
        errorException = e::class.qualifiedName,
      ),
    )
    if (e !is BadRequestException && e !is NotFoundException) {
      Sentry.captureException(e)
    }
  }

  private fun OutputStreamWriter.writeJson(data: Any) {
    val string = objectMapper.writeValueAsString(data)
    this.write(string + "\n")
    this.flush()
  }

  private val machineTranslationSuggestionFacade by lazy {
    applicationContext.getBean(MachineTranslationSuggestionFacade::class.java)
  }

  private val mtService by lazy {
    applicationContext.getBean(MtService::class.java)
  }

  private val projectHolder by lazy {
    applicationContext.getBean(ProjectHolder::class.java)
  }

  private val writer by lazy { OutputStreamWriter(outputStream) }

  private val mtTranslator by lazy {
    mtService.getMtTranslator(projectHolder.project.id, false)
  }

  private val objectMapper by lazy {
    applicationContext.getBean(ObjectMapper::class.java)
  }

  private val baseBlank by lazy {
    mtTranslator
      .getBaseTranslation(dto.keyId, dto.baseText)
      .isNullOrBlank()
  }
}
