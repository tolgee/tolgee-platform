package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.service.language.LanguageService
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
    val baseLanguage = languageService.getProjectBaseLanguage(projectId)

    val baseText =
      if (translation.language.id != baseLanguage.id) {
        translationService
          .getTranslations(
            listOf(translation.key.id),
            listOf(baseLanguage.id),
          ).firstOrNull()
          ?.text
      } else {
        null
      }

    val params =
      QaCheckParams(
        baseText = baseText,
        text = translation.text ?: "",
        baseLanguageTag = if (translation.language.id != baseLanguage.id) baseLanguage.tag else null,
        languageTag = translation.language.tag,
      )

    val results = qaCheckRunnerService.runChecks(projectId, params, checkTypes)
    qaIssueService.replaceIssuesForTranslation(translation, results, checkTypes)

    translation.qaChecksStale = false
    translationService.save(translation)
  }
}
