package io.tolgee.component

import io.tolgee.events.OnTranslationsSet
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class AutoTranslationListener(
  private val autoTranslationService: AutoTranslationService
) : Logging {

  @Order(2)
  @EventListener
  fun onApplicationEvent(event: OnTranslationsSet) {
    val baseLanguage = event.key.project.baseLanguage ?: return
    val wasUntranslatedBefore = event.oldValues[baseLanguage.tag].isNullOrEmpty()
    val isTranslatedAfter = !event.translations.find { it.language == baseLanguage }?.text.isNullOrEmpty()
    try {
      if (wasUntranslatedBefore && isTranslatedAfter) {
        autoTranslationService.autoTranslate(
          key = event.key,
          isBatch = true,
        )
      }
    } catch (e: OutOfCreditsException) {
      logger.debug("Auto translation failed because of out of credits")
    }
  }
}
