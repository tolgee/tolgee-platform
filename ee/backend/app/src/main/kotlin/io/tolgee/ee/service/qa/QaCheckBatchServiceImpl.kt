package io.tolgee.ee.service.qa

import io.tolgee.constants.Feature
import io.tolgee.formats.getPluralForms
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureRegistry
import io.tolgee.service.qa.QaCheckBatchService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Primary
@Service
class QaCheckBatchServiceImpl(
  private val qaCheckRunnerService: QaCheckRunnerService,
  private val qaIssueService: QaIssueService,
  private val translationService: TranslationService,
  private val languageService: LanguageService,
) : QaCheckBatchService {
  @Transactional
  override fun runChecksAndPersist(
    projectId: Long,
    translationId: Long,
    checkTypes: List<QaCheckType>?,
  ) {
    val translation = translationService.get(translationId)

    if (!ProjectFeatureRegistry.isEnabledOnProject(Feature.QA_CHECKS, translation.key.project)) {
      return
    }

    val baseLanguage = languageService.getProjectBaseLanguage(projectId)
    val isBaseLanguage = translation.language.id == baseLanguage.id

    val baseText =
      if (!isBaseLanguage) {
        translationService
          .getTranslations(
            listOf(translation.key.id),
            listOf(baseLanguage.id),
          ).firstOrNull()
          ?.text
      } else {
        null
      }

    val translationText = translation.text ?: ""
    val isPlural = translation.key.isPlural
    val textParsed = if (isPlural) getPluralForms(translationText) else null
    val baseParsed = if (isPlural && baseText != null) getPluralForms(baseText) else null

    val params =
      QaCheckParams(
        baseText = baseText,
        text = translationText,
        baseLanguageTag = if (!isBaseLanguage) baseLanguage.tag else null,
        languageTag = translation.language.tag,
        isPlural = isPlural,
        textVariants = textParsed?.forms,
        textVariantOffsets = textParsed?.offsets,
        baseTextVariants = baseParsed?.forms,
      )

    val results =
      qaCheckRunnerService.runEnabledChecks(
        projectId,
        params,
        checkTypes,
        languageId = translation.language.id,
      )
    qaIssueService.replaceIssuesForTranslation(translation, results, checkTypes)

    translation.qaChecksStale = false
    translationService.save(translation)

    qaIssueService.publishQaIssuesUpdated(translation)
  }
}
