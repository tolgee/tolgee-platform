package io.tolgee.api.v2.controllers.suggestion

import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.hateoas.machineTranslation.SuggestResultModel
import io.tolgee.hateoas.machineTranslation.TranslationItemModel
import io.tolgee.security.ProjectHolder
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditBucketService
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

    return catchingOutOfCredits(projectHolder.project.organizationOwnerId) {
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
    targetLanguage: LanguageDto,
  ): Map<MtServiceType, TranslationItemModel>? {
    val result =
      mtService.getMachineTranslations(
        projectHolder.project.id,
        false,
      ) {
        baseTranslationText = dto.baseText
        keyId = dto.keyId
        desiredServices = dto.services
        useAllEnabledServices = dto.services.isNullOrEmpty()
        targetLanguageId = targetLanguage.id
      }

    val resultData =
      result.associate { resultItem ->
        resultItem.service to TranslationItemModel(resultItem.translatedText.orEmpty(), resultItem.contextDescription)
      }

    if (result.all { it.baseBlank }) {
      return null
    }

    return resultData
  }

  fun <T> catchingOutOfCredits(
    organizationId: Long,
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
      val balance = mtCreditBucketService.getCreditBalances(organizationId)
      throw BadRequestException(
        Message.OUT_OF_CREDITS,
        listOf(
          balance.creditBalance,
          // This is the extra credits balance, which is not supported anymore
          0,
        ),
      )
    }
  }

  val SuggestRequestDto.targetLanguage
    get() = languageService.get(this.targetLanguageId, projectHolder.project.id)
}
