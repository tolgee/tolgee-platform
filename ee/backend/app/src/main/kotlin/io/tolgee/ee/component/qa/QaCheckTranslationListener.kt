package io.tolgee.ee.component.qa

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckRunnerService
import io.tolgee.ee.service.qa.QaIssueService
import io.tolgee.events.OnTranslationsSet
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.TranslationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaCheckTranslationListener(
  private val qaCheckRunnerService: QaCheckRunnerService,
  private val qaIssueService: QaIssueService,
  private val translationService: TranslationService,
  private val languageService: LanguageService,
) {
  @TransactionalEventListener
  @Async
  fun onTranslationsSet(event: OnTranslationsSet) {
    processTranslationsSet(event)
  }

  fun processTranslationsSet(event: OnTranslationsSet) {
    try {
      val projectId = event.key.project.id
      val baseLanguage = languageService.getProjectBaseLanguage(projectId)

      for (eventTranslation in event.translations) {
        val translation = translationService.get(eventTranslation.id)
        val text = translation.text ?: ""

        var baseTag: String? = baseLanguage.tag
        if (translation.language.tag == baseTag) {
          baseTag = null
        }

        var baseText: String? = null
        if (baseTag != null) {
          val baseTranslations =
            translationService.getTranslations(
              listOf(event.key.id),
              listOf(baseLanguage.id),
            )
          baseText = baseTranslations.firstOrNull()?.text
        }

        val params =
          QaCheckParams(
            baseText = baseText,
            text = text,
            baseLanguageTag = baseTag,
            languageTag = translation.language.tag,
          )

        val results = qaCheckRunnerService.runChecks(params)
        qaIssueService.replaceIssuesForTranslation(translation, results)
      }
    } catch (e: Exception) {
      logger.error("Failed to run QA checks for key ${event.key.id}", e)
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(QaCheckTranslationListener::class.java)
  }
}
