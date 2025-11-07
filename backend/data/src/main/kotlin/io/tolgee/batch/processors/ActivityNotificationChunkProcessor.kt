package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.ActivityNotificationBatchRequest
import io.tolgee.batch.request.ActivityNotificationRequest
import io.tolgee.model.batch.params.ActivityNotificationJobParams
import io.tolgee.service.activityNotification.ActivityNotificationService
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.coroutines.CoroutineContext

@Component
class ActivityNotificationChunkProcessor(
  private val activityNotificationService: ActivityNotificationService,
  private val entityManager: EntityManager,
) : ChunkProcessor<ActivityNotificationBatchRequest, ActivityNotificationJobParams, ActivityNotificationRequest> {
  override fun process(
    job: BatchJobDto,
    chunk: List<ActivityNotificationRequest>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
  ) {
    val subChunked = chunk.chunked(1000)
    var progress = 0
    val params = getParams(job)
    val projectId = params.projectId ?: return
    val originatingUserId = params.originatingUserId ?: return

    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      val grouped = subChunk.groupBy { it.activityType }
      grouped.forEach { (activityType, items) ->
        val entityIds = items.map { it.entityId }
        activityNotificationService.sendActivityNotifications(
          projectId = projectId,
          originatingUserId = originatingUserId,
          activityType = activityType,
          entityIds = entityIds,
        )
      }
      entityManager.flush()
      progress += subChunk.size
      onProgress.invoke(progress)
    }
  }

  override fun getTarget(data: ActivityNotificationBatchRequest): List<ActivityNotificationRequest> = data.items

  override fun getParamsType(): Class<ActivityNotificationJobParams> = ActivityNotificationJobParams::class.java

  override fun getTargetItemType(): Class<ActivityNotificationRequest> = ActivityNotificationRequest::class.java

  override fun getExecuteAfter(data: ActivityNotificationBatchRequest): Date? =
    Date.from(Instant.now().plus(5, ChronoUnit.SECONDS)) // TODO: Change to 5 minutes

  override fun getParams(data: ActivityNotificationBatchRequest): ActivityNotificationJobParams =
    ActivityNotificationJobParams().apply {
      projectId = data.items.firstOrNull()?.projectId
      originatingUserId = data.items.firstOrNull()?.originatingUserId
      activityType = data.items.firstOrNull()?.activityType
    }

  override fun getJobCharacter(): JobCharacter = JobCharacter.FAST

  override fun getMaxPerJobConcurrency(): Int = 1
}
