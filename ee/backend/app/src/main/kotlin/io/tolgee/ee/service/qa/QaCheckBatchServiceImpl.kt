package io.tolgee.ee.service.qa

import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.constants.Feature
import io.tolgee.formats.getPluralForms
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureRegistry
import io.tolgee.service.project.ProjectService
import io.tolgee.service.qa.QaCheckBatchService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.executeInNewRepeatableTransaction
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Primary
@Service
class QaCheckBatchServiceImpl(
  private val qaCheckRunnerService: QaCheckRunnerService,
  private val qaIssueService: QaIssueService,
  private val translationService: TranslationService,
  private val translationRepository: TranslationRepository,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val businessEventPublisher: BusinessEventPublisher,
  private val transactionManager: PlatformTransactionManager,
  private val projectService: ProjectService,
) : QaCheckBatchService {
  @Transactional
  override fun runChecksAndPersist(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    checkTypes: List<QaCheckType>?,
  ) {
    val project = projectService.getDto(projectId)

    if (!ProjectFeatureRegistry.isEnabledOnProject(Feature.QA_CHECKS, project)) {
      return
    }

    val existingTranslation =
      translationRepository.findOneByProjectIdAndKeyIdAndLanguageId(projectId, keyId, languageId)

    val baseLanguage = languageService.getProjectBaseLanguage(projectId)
    val isBaseLanguage = languageId == baseLanguage.id
    val language = if (isBaseLanguage) baseLanguage else languageService.getEntity(languageId, projectId)

    val baseText =
      if (!isBaseLanguage) {
        translationService
          .getTranslations(
            listOf(keyId),
            listOf(baseLanguage.id),
          ).firstOrNull()
          ?.text
      } else {
        null
      }

    val translationText = existingTranslation?.text ?: ""
    val key = keyService.get(keyId)
    val isPlural = key.isPlural
    val textParsed = if (isPlural) getPluralForms(translationText) else null
    val baseParsed = if (isPlural && baseText != null) getPluralForms(baseText) else null

    val params =
      QaCheckParams(
        baseText = baseText,
        text = translationText,
        baseLanguageTag = if (!isBaseLanguage) baseLanguage.tag else null,
        languageTag = language.tag,
        isPlural = isPlural,
        textVariants = textParsed?.forms,
        textVariantOffsets = textParsed?.offsets,
        baseTextVariants = baseParsed?.forms,
        maxCharLimit = key.maxCharLimit,
        icuPlaceholders = project.icuPlaceholders,
      )

    val results =
      qaCheckRunnerService.runEnabledChecks(
        projectId,
        params,
        checkTypes,
        languageId = languageId,
      )

    if (results.isEmpty() && existingTranslation == null) return

    executeInNewRepeatableTransaction(transactionManager) {
      val translation = translationService.getOrCreate(projectId, keyId, languageId)

      qaIssueService.replaceIssuesForTranslation(translation, results, checkTypes)

      if (checkTypes == null) {
        translation.qaChecksStale = false
      }
      translationService.save(translation)

      qaIssueService.publishQaIssuesUpdated(translation)
    }

    publishBusinessEvent(projectId)
  }

  private fun publishBusinessEvent(projectId: Long) {
    businessEventPublisher.publishOnceInTime(
      OnBusinessEventToCaptureEvent(
        eventName = "QA_CHECK_RUN",
        projectId = projectId,
      ),
      Duration.ofDays(1),
    ) {
      "QA_CHECK_RUN_$projectId"
    }
  }
}
