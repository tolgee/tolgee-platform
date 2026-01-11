package io.tolgee.component.eventListeners

import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.service.project.ProjectService
import io.tolgee.util.runSentryCatching
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LanguageStatsListener(
  private var languageStatsService: LanguageStatsService,
  private val projectService: ProjectService,
) {
  var bypass = false

  @TransactionalEventListener
  @Async
  fun onActivity(event: OnProjectActivityEvent) {
    if (bypass) return
    runSentryCatching {
      val projectId = event.activityRevision.projectId ?: return

      // exit when project doesn't exist anymore
      projectService.findDto(projectId) ?: return

      val modifiedEntityClasses = event.modifiedEntities.keys.toSet()
      val isStatsModified =
        modifiedEntityClasses.contains(Language::class) ||
          modifiedEntityClasses.contains(Translation::class) ||
          modifiedEntityClasses.contains(Key::class) ||
          modifiedEntityClasses.contains(Project::class)

      if (isStatsModified) {
        languageStatsService.refreshLanguageStats(projectId)
      }
    }
  }
}
