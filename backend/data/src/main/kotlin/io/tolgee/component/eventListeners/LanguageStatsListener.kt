package io.tolgee.component.eventListeners

import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.util.runSentryCatching
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class LanguageStatsListener(
  private var languageStatsService: LanguageStatsService
) {
  @EventListener
  @Async
  fun onActivity(event: OnProjectActivityEvent) {
    runSentryCatching {
      val projectId = event.activityRevision.projectId ?: return

      val modifiedEntityClasses = event.modifiedEntities.keys.toSet()
      val isStatsModified = modifiedEntityClasses.contains(Language::class) ||
        modifiedEntityClasses.contains(Translation::class) ||
        modifiedEntityClasses.contains(Key::class) ||
        modifiedEntityClasses.contains(Project::class)

      if (isStatsModified) {
        languageStatsService.refreshLanguageStats(projectId)
      }
    }
  }
}
