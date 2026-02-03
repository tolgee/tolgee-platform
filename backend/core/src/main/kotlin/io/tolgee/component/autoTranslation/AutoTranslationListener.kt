package io.tolgee.component.autoTranslation

import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.util.Logging
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class AutoTranslationListener(
  private val applicationContext: ApplicationContext,
) : Logging {
  @Order(2)
  @EventListener
  fun onApplicationEvent(event: OnProjectActivityStoredEvent) {
    AutoTranslationEventHandler(event, applicationContext).handle()
  }
}
