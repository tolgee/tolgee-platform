package io.tolgee.component

import io.tolgee.events.OnTranslationsSet
import io.tolgee.service.translation.AutoTranslationService
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class AutoTranslationListener(
  private val autoTranslationService: AutoTranslationService,
) {

  @Order(2)
  @EventListener
  fun onApplicationEvent(event: OnTranslationsSet) {
    val baseLanguage = event.key.project.baseLanguage ?: return
    val wasUntranslatedBefore = event.oldValues[baseLanguage.tag].isNullOrEmpty()
    val isTranslatedAfter = !event.translations.find { it.language == baseLanguage }?.text.isNullOrEmpty()
    if (wasUntranslatedBefore && isTranslatedAfter) {
      autoTranslationService.autoTranslate(
        key = event.key,
      )
    }
  }
}
