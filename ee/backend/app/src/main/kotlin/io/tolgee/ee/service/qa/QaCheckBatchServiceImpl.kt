package io.tolgee.ee.service.qa

import io.sentry.Sentry
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.service.glossary.GlossaryTermService
import io.tolgee.formats.getPluralForms
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.TranslationRepository
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
  private val keyRepository: KeyRepository,
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
  ) {
    runChecksAndPersistChunk(
      projectId = projectId,
      checkTypes = checkTypes,
      items = listOf(BatchTranslationTargetItem(keyId = keyId, languageId = languageId)),
    )
  }

  override fun runChecksAndPersistChunk(
    projectId: Long,
    checkTypes: List<QaCheckType>?,
    items: List<BatchTranslationTargetItem>,
    progressCallback: () -> Unit,
  ) {
    if (items.isEmpty()) return
    val project = projectService.getDto(projectId)
    if (!ProjectFeatureRegistry.isEnabledOnProject(Feature.QA_CHECKS, project)) {
      return
    }

    val results = runChecksForChunk(project, checkTypes, items, progressCallback)
    persistChunkResults(projectId, results, checkTypes)
    publishBusinessEvent(projectId)
  }

  private fun runChecksForChunk(
    project: ProjectDto,
    checkTypes: List<QaCheckType>?,
    items: List<BatchTranslationTargetItem>,
    progressCallback: () -> Unit,
  ): List<ItemResult> {
    val qaDisabledLanguageIds = projectQaConfigService.getDisabledLanguageIds(project.id)
    val enabledItems = items.filter { it.languageId !in qaDisabledLanguageIds }

    val baseLanguage = languageService.getProjectBaseLanguage(project.id)
    val keyIds = enabledItems.map { it.keyId }.toSet()
    val languageIds = enabledItems.map { it.languageId }.toSet()

    val translations =
      translationRepository.findAllWithKeyAndLanguageByKeyIdInAndLanguageIdIn(
        keyIds,
        languageIds + baseLanguage.id,
      )

    val wantedPairs = enabledItems.map { it.keyId to it.languageId }.toSet()
    val translationByKeyAndLanguage =
      translations
        .filter { it.key.id to it.language.id in wantedPairs }
        .associateBy { it.key.id to it.language.id }

    val baseTextByKey =
      translations
        .filter { it.language.id == baseLanguage.id }
        .associate { it.key.id to it.text }

    val tagsFromTranslations =
      translations.associate { it.language.id to it.language.tag } + (baseLanguage.id to baseLanguage.tag)
    val missingLanguageIds = languageIds.filter { it !in tagsFromTranslations }
    val missingLanguageTags = languageService.findByIdIn(missingLanguageIds).associate { it.id to it.tag }
    val languageTagById = tagsFromTranslations + missingLanguageTags

    val keyById = keyRepository.findAllByIdIn(keyIds).associateBy { it.id }

    val enabledCheckTypesByLanguage: Map<Long, Set<QaCheckType>> =
      languageIds.associateWith { projectQaConfigService.getEnabledCheckTypesForLanguage(project.id, it) }

    return enabledItems.mapNotNull { item ->
      // If the key has been deleted between job enqueue and processing, skip it
      val key = keyById[item.keyId] ?: return@mapNotNull null
      val languageTag = languageTagById[item.languageId] ?: return@mapNotNull null

      val translation = translationByKeyAndLanguage[item.keyId to item.languageId]
      val text = translation?.text ?: ""
      val isBaseLanguage = item.languageId == baseLanguage.id
      val baseText = if (isBaseLanguage) null else baseTextByKey[item.keyId]
      val isPlural = key.isPlural

      val textParsed =
        text.takeIf { isPlural }?.let {
          runCatching { getPluralForms(it) }.getOrNull()
        }
      val baseParsed =
        baseText?.takeIf { isPlural }?.let {
          runCatching { getPluralForms(it) }.getOrNull()
        }

      // This is probably the most expensive thing we do for each item
      // In the future, we will probably switch to persisting glossary terms for each translation,
      // so optimizing it right now might be premature
      val glossaryTerms =
        findGlossaryTerms(project.organizationOwnerId, project.id, text, languageTag)

      val params =
        QaCheckParams(
          baseText = baseText,
          text = text,
          baseLanguageTag = if (!isBaseLanguage) baseLanguage.tag else null,
          languageTag = languageTag,
          isPlural = isPlural,
          textVariants = textParsed?.forms,
          textVariantOffsets = textParsed?.offsets,
          baseTextVariants = baseParsed?.forms,
          maxCharLimit = key.maxCharLimit,
          icuPlaceholders = project.icuPlaceholders,
          glossaryTerms = glossaryTerms,
        )

      val enabledCheckTypes = enabledCheckTypesByLanguage[item.languageId]
      val results =
        qaCheckRunnerService.runEnabledChecks(
          project.id,
          params,
          checkTypes,
          languageId = item.languageId,
          enabledCheckTypes = enabledCheckTypes,
        )

      progressCallback()

      ItemResult(
        keyId = item.keyId,
        languageId = item.languageId,
        languageTag = languageTag,
        textSnapshot = text,
        hadExistingTranslation = translation != null,
        results = results,
      )
    }
  }

  private fun persistChunkResults(
    projectId: Long,
    results: List<ItemResult>,
    checkTypes: List<QaCheckType>?,
  ) {
    executeInNewRepeatableTransaction(transactionManager) {
      results
        .mapNotNull { result ->
          // Create / Get translation entities

          if (result.results.isEmpty() && !result.hadExistingTranslation) return@mapNotNull null

          // Second most expensive operation here - we deal with each translation separately
          // Might be worth batching this in single query in future
          val translation = translationService.getOrCreate(projectId, result.keyId, result.languageId)

          // Only clear the stale flag if the translation text hasn't changed since we collected inputs.
          // If it changed, QaActivityListener already marked it stale again, and a new batch job
          // will re-check with the updated text.
          if (checkTypes == null && (translation.text ?: "") == result.textSnapshot) {
            translation.qaChecksStale = false
          }

          // Disable activity logging — no content changes are happening here.
          // Without this, when we create a new empty translation (so we can reference it from QA issues),
          // it gets logged.
          translation.disableActivityLogging = true
          translationService.save(translation)

          translation to result
        }.also {
          // Save QA issues

          val resultsByTranslationId = it.associate { (t, itemResult) -> t.id to itemResult.results }
          qaIssueService.replaceIssuesForTranslations(
            resultsByTranslationId = resultsByTranslationId,
            checkTypes = checkTypes,
          )
        }.map { (translation, result) ->
          // Create events for updated QA issues

          QaIssuesUpdatedEvent(translation.id, result.keyId, result.languageTag, translation.qaChecksStale)
        }.also { events ->
          // Send events

          qaIssueService.publishQaIssuesUpdated(projectId, events)
        }
    }
  }

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

    private data class ItemResult(
      val keyId: Long,
      val languageId: Long,
      val languageTag: String,
      val textSnapshot: String,
      val hadExistingTranslation: Boolean,
      val results: List<QaCheckResult>,
    )
  }
}
