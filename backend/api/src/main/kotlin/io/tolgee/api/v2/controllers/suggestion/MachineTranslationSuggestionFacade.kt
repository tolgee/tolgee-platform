package io.tolgee.api.v2.controllers.suggestion

import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.MtCreditBalanceDto
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.hateoas.machineTranslation.SuggestResultModel
import io.tolgee.hateoas.machineTranslation.TranslationItemModel
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.security.ProjectHolder
import io.tolgee.service.LanguageService
import io.tolgee.service.key.KeyService
import io.tolgee.service.machineTranslation.MtCreditBucketService
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.security.SecurityService
import io.tolgee.util.StreamingResponseBodyProvider
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@Service
class MachineTranslationSuggestionFacade(
  private val projectHolder: ProjectHolder,
  private val mtService: MtService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val securityService: SecurityService,
  private val mtCreditBucketService: MtCreditBucketService,
  private val applicationContext: ApplicationContext,
  private val streamingResponseBodyProvider: StreamingResponseBodyProvider,
) {
  fun suggestSync(dto: SuggestRequestDto): SuggestResultModel {
    val targetLanguage = dto.targetLanguage

    securityService.checkLanguageTranslatePermission(
      projectHolder.project.id,
      listOf(targetLanguage.id),
    )

    val balanceBefore = mtCreditBucketService.getCreditBalances(projectHolder.project.organizationOwnerId)

    return catchingOutOfCredits(balanceBefore) {
      val resultData = getTranslationResults(dto, targetLanguage)
      val baseBlank = resultData == null

      SuggestResultModel(
        machineTranslations = resultData?.map { it.key to it.value.output }?.toMap(),
        result = resultData,
        baseBlank = baseBlank,
      )
    }
  }

  @Transactional
  fun suggestStreaming(dto: SuggestRequestDto): StreamingResponseBody {
    val streamer = MtResultStreamer(dto, applicationContext, streamingResponseBodyProvider)
    return streamer.stream()
  }

  /**
   * Returns the translated value for the given dto.
   * Returns null only if base text is empty
   */
  private fun getTranslationResults(
    dto: SuggestRequestDto,
    targetLanguage: Language,
  ): Map<MtServiceType, TranslationItemModel>? {
    val key = dto.key

    val resultMap =
      mtService.getMachineTranslations(
        projectHolder.projectEntity,
        key,
        dto.baseText,
        targetLanguage,
        dto.services,
      )

    val resultData =
      resultMap
        .map { (key, value) ->
          key to TranslationItemModel(value.translatedText.orEmpty(), value.contextDescription)
        }.toMap()

    if (resultMap.values.all { it.baseBlank }) {
      return null
    }

    return resultData
  }

  fun <T> catchingOutOfCredits(
    balanceBefore: MtCreditBalanceDto,
    fn: () -> T,
  ): T {
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
        listOf(balanceBefore.creditBalance, balanceBefore.extraCreditBalance),
      )
    }
  }

  val SuggestRequestDto.key
    get() =
      if (this.baseText != null) {
        null
      } else {
        keyService.findOptional(this.keyId).orElseThrow { NotFoundException(Message.KEY_NOT_FOUND) }?.also {
          it.checkInProject()
        }
      }

  val SuggestRequestDto.targetLanguage
    get() = languageService.get(this.targetLanguageId)

  private fun Key.checkInProject() {
    keyService.checkInProject(this, projectHolder.project.id)
  }
}
