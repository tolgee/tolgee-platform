package io.tolgee.ee.service.qa

import io.sentry.Sentry
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.constants.Feature
import io.tolgee.ee.service.glossary.GlossaryTermService
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
import org.slf4j.LoggerFactory
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
  private val glossaryTermService: GlossaryTermService,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val projectQaConfigService: ProjectQaConfigService,
) : QaCheckBatchService {
  @Transactional
  override fun runChecksAndPersist(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    checkTypes: List<QaCheckType>?,
    enabledCheckTypes: Set<QaCheckType>?,
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
    val textParsed =
      translationText.takeIf { isPlural }?.let {
        runCatching { getPluralForms(it) }.getOrNull()
      }
    val baseParsed =
      baseText?.takeIf { isPlural }?.let {
        runCatching { getPluralForms(it) }.getOrNull()
      }

    val glossaryTerms = findGlossaryTerms(project.organizationOwnerId, projectId, translationText, language.tag)

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
        glossaryTerms = glossaryTerms,
      )

    val results =
      qaCheckRunnerService.runEnabledChecks(
        projectId,
        params,
        checkTypes,
        languageId = languageId,
        enabledCheckTypes = enabledCheckTypes,
      )

    if (results.isEmpty() && existingTranslation == null) return

    executeInNewRepeatableTransaction(transactionManager) {
      val translation = translationService.getOrCreate(projectId, keyId, languageId)

      // Only clear the stale flag if the translation text hasn't changed since we collected inputs.
      // If it changed, QaActivityListener already marked it stale again, and a new batch job
      // will re-check with the updated text.
      if (checkTypes == null && (translation.text ?: "") == translationText) {
        translation.qaChecksStale = false
      }

      // Disable activity logging — no content changes are happening here.
      // Without this, when we create a new empty translation (so we can reference it from QA issues),
      // it gets logged.
      translation.disableActivityLogging = true
      translationService.save(translation)

      qaIssueService.replaceIssuesForTranslation(translation, results, checkTypes)

      qaIssueService.publishQaIssuesUpdated(translation)
    }

    publishBusinessEvent(projectId)
  }

  override fun getEnabledCheckTypesForLanguage(
    projectId: Long,
    languageId: Long,
  ): Set<QaCheckType> = projectQaConfigService.getEnabledCheckTypesForLanguage(projectId, languageId)

  private fun findGlossaryTerms(
    organizationOwnerId: Long,
    projectId: Long,
    text: String,
    languageTag: String,
  ): List<QaGlossaryTerm>? {
    if (text.isEmpty() || !enabledFeaturesProvider.isFeatureEnabled(organizationOwnerId, Feature.GLOSSARY)) {
      return null
    }
    return try {
      glossaryTermService.findQaGlossaryTerms(organizationOwnerId, projectId, text, languageTag)
    } catch (e: Exception) {
      Sentry.captureException(e)
      logger.warn("Glossary lookup failed for project {}", projectId, e)
      null
    }
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

  companion object {
    private val logger = LoggerFactory.getLogger(QaCheckBatchServiceImpl::class.java)
  }
}
