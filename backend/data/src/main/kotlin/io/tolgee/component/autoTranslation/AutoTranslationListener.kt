package io.tolgee.component.autoTranslation

import com.google.cloud.translate.Translation
import io.tolgee.activity.data.ActivityType
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.key.Key
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.util.Logging
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class AutoTranslationListener(
  private val applicationContext: ApplicationContext
) : Logging {

  @Order(2)
  @EventListener
  fun onApplicationEvent(event: OnProjectActivityStoredEvent) {
    AutoTranslationEventHandler(event, applicationContext).handle()
  }
}
