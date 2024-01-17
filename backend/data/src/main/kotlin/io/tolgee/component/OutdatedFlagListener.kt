package io.tolgee.component

import io.tolgee.events.OnTranslationsSet
import io.tolgee.service.translation.TranslationService
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class OutdatedFlagListener(
  private val translationService: TranslationService,
) {
  @EventListener
  @Order(1)
  fun onEvent(event: OnTranslationsSet) {
    val baseLanguage = event.key.project.baseLanguage ?: return
    val oldBaseValue = event.oldValues[baseLanguage.tag]
    val newBaseValue = event.translations.find { it.language.id == baseLanguage.id }?.text
    if (oldBaseValue != newBaseValue) {
      val excluded = event.translations.map { it.id }.toSet()
      translationService.setOutdated(event.key, excluded)
    }
  }
}
