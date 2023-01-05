package io.tolgee.service

import io.tolgee.events.OnEntityPrePersist
import io.tolgee.model.Language
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class CockroachStatsTestListener(
  private val organizationStatsService: OrganizationStatsService
) {

  @EventListener
  fun onLanguageAdd(event: OnEntityPrePersist) {
    (event.entity as? Language)?.let {
      organizationStatsService.getCurrentTranslationCount(event.entity.project.organizationOwner.id)
    }
  }
}
