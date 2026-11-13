package io.tolgee.service.notification.activity

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.batch.request.NotificationFanoutRequest
import io.tolgee.component.eventListeners.BypassableActivityListener
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import jakarta.persistence.EntityManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class NotificationFanoutListener(
  private val batchJobService: BatchJobService,
  private val changeQueries: NotificationChangeQueries,
  private val entityManager: EntityManager,
) : BypassableActivityListener {
  @Volatile
  override var bypass = false

  @EventListener
  fun onActivityStored(event: OnProjectActivityStoredEvent) {
    if (bypass) return
    val revision = event.activityRevision
    if (revision.batchJobChunkExecution != null) return
    val projectId = revision.projectId ?: return
    if (revision.modifiedEntities.none { it.entityClass in NOTIFYING_ENTITY_CLASSES }) return
    startFanout(projectId, revision.id)
  }

  @EventListener
  fun onBatchJobFinalized(event: OnBatchJobFinalized) {
    if (bypass) return
    val revisionId = event.activityRevisionId ?: return
    val projectId = event.job.projectId ?: return
    if (changeQueries.typesFor(event.job.type).isEmpty()) return
    startFanout(projectId, revisionId)
  }

  private fun startFanout(
    projectId: Long,
    revisionId: Long,
  ) {
    batchJobService.startJob(
      request = NotificationFanoutRequest(revisionId),
      project = entityManager.getReference(Project::class.java, projectId),
      author = null,
      type = BatchJobType.NOTIFICATION_FANOUT,
      isHidden = true,
    )
  }

  companion object {
    private val NOTIFYING_ENTITY_CLASSES = setOf(Key::class.simpleName, Translation::class.simpleName)
  }
}
